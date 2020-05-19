package com.datarobot.examples.scoring.prime;//
// -*- coding: UTF-8 -*-
// Copyright @2020. DataRobot, Inc. All Rights Reserved. Permission to use, copy, modify,
// and distribute this software and its documentation is hereby granted, provided that the
// above copyright notice, this paragraph and the following two paragraphs appear in all copies,
// modifications, and distributions of this software or its documentation. Contact DataRobot,
// 225 Franklin Street, Boston, MA, United States 02110, support@datarobot.com
// for more details.
//
// IN NO EVENT SHALL DATAROBOT BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
// OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS OR LOST DATA, ARISING OUT OF THE USE OF THIS
// SOFTWARE AND ITS DOCUMENTATION BASED ON ANY THEORY OF LIABILITY, EVEN IF DATAROBOT HAS BEEN
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// THE SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS IS".
// DATAROBOT SPECIFICALLY DISCLAIMS ANY AND ALL WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  DATAROBOT HAS NO
// OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
//
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.regex.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.RuntimeException;
import java.lang.IllegalAccessException;
import java.lang.UnsupportedOperationException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Prediction {

    public enum VarTypes {
        DATE, CATEGORY, NUMERIC, TEXT, CATEGORY_INT
    }
    
    private static final String STRING_NA_VALUE = null;
    private static final long ORIGIN = -62135596800000L;
    private static final Pattern CURRENCY_PATTERN =
        Pattern.compile("[\\$\u00A3\uFFE1\u20AC\u00A5\uFFE5,]|(EUR)");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)" +
            "([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}" +
            "+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)" +
            "(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*");
    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile("^[Hhm:]+[ms:]s?( a)?$");
    private static final TimeZone UTC_TIMEZONE =
        new SimpleTimeZone(0, "UTC");
    private static final Calendar UTC_EN_US_CALENDAR =
        Calendar.getInstance(UTC_TIMEZONE, Locale.forLanguageTag("en-US"));
    private static final Map<String, DateFormat> DATE_PARSERS = new HashMap();
    private static final double ONE_PER_DAY_IN_MS = 1.0/(60.0 * 60.0 * 24.0 * 1000.0);
    private static final DecimalFormat decimalFmt =
        new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static Method fastMathExp = null;
    private static Method guavaDoublesTryParse = null;
    private static Method guavaFloatsTryParse = null;

    // dynamic loading the commons CSV parsers
    private static Method CSVRecordGet = null;
    private static Method CSVRecordSize = null;
    private static Method CSVFormatParse = null;
    private static Method CSVParserIterator = null;
    private static Field CSVFormatEXCEL = null;

    private Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class clazz = this.getClass(className);
            if (clazz == null) return null;
            return clazz.getMethod(methodName, parameterTypes);
        } catch(NoSuchMethodException e) {
        }
        return null;
    }

    private Field getField(String className, String fieldName) {
        try {
            Class clazz = this.getClass(className);
            if (clazz == null) return null;
            return clazz.getField(fieldName);
        } catch(NoSuchFieldException e) {
        }
        return null;
    }

    private Class getClass(String className) {
        try {
            return Class.forName(className);
        } catch(ClassNotFoundException e) {
        }
        return null;
    }

    public Prediction() {
        UTC_EN_US_CALENDAR.setMinimalDaysInFirstWeek(7);
        ((GregorianCalendar)UTC_EN_US_CALENDAR).setGregorianChange(new Date(Long.MIN_VALUE));
        UTC_EN_US_CALENDAR.clear();
        decimalFmt.setMaximumFractionDigits(12);

        fastMathExp = this.getMethod(
            "org.apache.commons.math3.util.FastMath",
            "exp",
            Double.TYPE
        );
        guavaDoublesTryParse = this.getMethod(
            "com.google.common.primitives.Doubles",
            "tryParse",
            String.class
        );
        guavaFloatsTryParse = this.getMethod(
            "com.google.common.primitives.Floats",
            "tryParse",
            String.class
        );

        CSVRecordGet = this.getMethod(
            "org.apache.commons.csv.CSVRecord",
            "get",
            Integer.TYPE
        );
        CSVRecordSize = this.getMethod(
            "org.apache.commons.csv.CSVRecord",
            "size"
        );
        CSVFormatParse = this.getMethod(
            "org.apache.commons.csv.CSVFormat",
            "parse",
            Reader.class
        );
        CSVParserIterator = this.getMethod(
            "org.apache.commons.csv.CSVParser",
            "iterator"
        );
        CSVFormatEXCEL = this.getField("org.apache.commons.csv.CSVFormat", "EXCEL");
    }

    private double exp(double d) {
        if (fastMathExp != null) {
            try {
                return (Double) fastMathExp.invoke(null, d);
            } catch (Exception e) {
                fastMathExp = null;
                return this.exp(d);
            }
        } else {
            return Math.exp(d);
        }
    }

    private static float stringToFloat(String str) {
        return stringToFloat(str, Float.NaN);
    }

    private static float stringToFloat(String str, float replacement) {
        if (guavaFloatsTryParse != null) {
            try {
                Float floatVal = (Float) guavaFloatsTryParse.invoke(null, str);
                if (floatVal == null)
                    return replacement;
                return floatVal;
            } catch (Exception e) {
                guavaFloatsTryParse = null;
                return stringToFloat(str, replacement);
            }
        } else {
            try {
                return Float.parseFloat(str);
            } catch (Exception e) {
                return replacement;
            }
        }
    }

    private static double stringToDouble(String x) {
        return stringToDouble(x, Double.NaN);
    }

    private static double stringToDouble(String x, double replacement) {
        if (guavaDoublesTryParse != null) {
            try {
                Double doubleVal = (Double) guavaDoublesTryParse.invoke(null, x);
                if (doubleVal == null)
                    return replacement;
                return doubleVal;
            } catch (Exception e) {
                guavaDoublesTryParse = null;
                return stringToDouble(x, replacement);
            }
        } else {
            try {
                return Double.parseDouble(x);
            } catch(Exception e) {
                return replacement;
            }
        }
    }
    
    private static SimpleDateFormat getDateFormatParser(String format) {
        SimpleDateFormat parser = (SimpleDateFormat)DATE_PARSERS.get(format);
        if (parser == null) {
            parser = new SimpleDateFormat(format, Locale.ENGLISH);
            parser.setCalendar(UTC_EN_US_CALENDAR);
            UTC_EN_US_CALENDAR.set(1969, 0, 1);
            parser.set2DigitYearStart(UTC_EN_US_CALENDAR.getTime());
            UTC_EN_US_CALENDAR.clear();
            DATE_PARSERS.put(format, parser);
        }
        return parser;
    }

    private static Date maybeFixDate(Date date, String dateStr, String format)
            throws ParseException {
        Date correctDate = date;
        if (format.contains("s.S")) {
            char[] dateChars = dateStr.toCharArray();
            int startMileseconds = -1;
            int endMileseconds = -1;
            int charIndex = dateStr.lastIndexOf('.');

            if (charIndex < 0)
                charIndex = 0;

            for(; charIndex < dateChars.length; charIndex++) {
                char currentChar = dateChars[charIndex];
                if(currentChar != '.' && startMileseconds < 0) {
                    continue;
                } else if (currentChar == '.' && startMileseconds < 0) {
                    startMileseconds = charIndex + 1;
                    continue;
                }
                if(!Character.isDigit(currentChar)) {
                    endMileseconds = charIndex;
                    break;
                }
            }
            if(endMileseconds < 0) {
                endMileseconds = dateChars.length;
            }
            int length = endMileseconds - startMileseconds;

            char[] msDigits = new char[3];
            for(int digitIndex = 0; digitIndex < 3; digitIndex++) {
                char value = '0';
                if(digitIndex < length) {
                    value = dateChars[startMileseconds+digitIndex];
                }
                msDigits[digitIndex] = value;
            }
            dateStr = dateStr.substring(0, startMileseconds)
                + new String(msDigits)
                + dateStr.substring(endMileseconds, dateChars.length);

            SimpleDateFormat simpleParser = getDateFormatParser(format);
            correctDate = simpleParser.parse(dateStr);
        }
        return correctDate;
    }
        private double score(Map row) {
        double row_pred = -0.4236989+
            score1(row)+
            score2(row);
        return row_pred;
    }
    private double score1(Map row) {
        String String_rosiglitazone = (String)row.get("rosiglitazone");
        String String_medical_specialty = (String)row.get("medical_specialty");
        float number_diagnoses = colAsNumeric(row, "number_diagnoses");
        float num_medications = colAsNumeric(row, "num_medications");
        String String_pioglitazone = (String)row.get("pioglitazone");
        float number_inpatient = colAsNumeric(row, "number_inpatient");
        String String_race = (String)row.get("race");
        String String_discharge_disposition_id = (String)row.get("discharge_disposition_id");
        String String_weight = (String)row.get("weight");
        String String_glimepiride = (String)row.get("glimepiride");
        String String_repaglinide = (String)row.get("repaglinide");
        String String_diag_3 = (String)row.get("diag_3");
        String String_diag_2 = (String)row.get("diag_2");
        String String_diag_1 = (String)row.get("diag_1");
        String String_admission_source_id = (String)row.get("admission_source_id");
        float number_outpatient = colAsNumeric(row, "number_outpatient");
        String String_insulin = (String)row.get("insulin");
        String String_diag_1_desc = (String)row.get("diag_1_desc");
        String String_chlorpropamide = (String)row.get("chlorpropamide");
        String String_A1Cresult = (String)row.get("A1Cresult");
        String String_glipizide = (String)row.get("glipizide");
        float time_in_hospital = colAsNumeric(row, "time_in_hospital");
        String String_change = (String)row.get("change");
        String String_payer_code = (String)row.get("payer_code");
        String String_diabetesMed = (String)row.get("diabetesMed");
        float num_lab_procedures = colAsNumeric(row, "num_lab_procedures");
        String String_admission_type_id = (String)row.get("admission_type_id");
        String String_glyburide_metformin = (String)row.get("glyburide_metformin");
        String String_age = (String)row.get("age");
        String String_diag_3_desc = (String)row.get("diag_3_desc");
        float number_emergency = colAsNumeric(row, "number_emergency");
        String String_max_glu_serum = (String)row.get("max_glu_serum");
        String String_gender = (String)row.get("gender");
        String String_diag_2_desc = (String)row.get("diag_2_desc");
        float num_procedures = colAsNumeric(row, "num_procedures");
        String String_metformin = (String)row.get("metformin");
        return
          -0.027771337644286592933 * ((matchWord("congestive", String_diag_3_desc))?1:0) +
           0.042254989515401679412 * ((matchWord("diverticulosis", String_diag_1_desc))?1:0) +
           0.055983806803230319604 * ((String_diag_2.equals("276"))?1:0) +
           0.036808866813061398571 * ((String_payer_code.equals("MD"))?1:0) +
          -0.097620972209088080485 * ((String_diag_1.equals("250.02"))?1:0) +
         0.00029376188745485658574 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_age.equals("[80-90)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     String_race.equals("Caucasian"))?1:0) +
        -0.00065043093617830763722 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     ! String_age.equals("[80-90)") && 
                                     ! String_race.equals("Caucasian") && 
                                     number_emergency <= 2.5)?1:0) +
             0.1490490653952495359 * ((String_diag_2.equals("401"))?1:0) +
          0.0033434782563387640056 * ((matchWord("disorder", String_diag_1_desc))?1:0) +
          -0.055320816967291769339 * ((matchWord("inhalation", String_diag_1_desc))?1:0) +
          -0.073558939891327945748 * ((matchWord("coronary", String_diag_1_desc))?1:0) +
          0.0012197232700172208533 * ((matchWord("disease", String_diag_3_desc))?1:0) +
        -8.6629105733788464754E-05 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_medical_specialty.equals("Cardiology") && 
                                     num_procedures > 0.5 && 
                                     num_medications > 4.5)?1:0) +
           -0.15277368127009618992 * ((String_race.equals("NaN"))?1:0) +
          0.0064916102952238333157 * ((String_A1Cresult.equals("NaN"))?1:0) +
           0.036732632350800376031 * ((! String_admission_type_id.equals("Emergency") && 
                                     String_diabetesMed.equals("Yes") && 
                                     ! String_glipizide.equals("Up") && 
                                     ! String_race.equals("NaN"))?1:0) +
          0.0042012926343354198289 * ((matchWord("failure", String_diag_1_desc))?1:0) +
         0.00089359548927137391616 * ((matchWord("respiratory", String_diag_1_desc))?1:0) +
          -0.032078185772058398495 * ((! String_admission_type_id.equals("Not Available") && 
                                     ! String_max_glu_serum.equals("Norm") && 
                                     ! String_medical_specialty.equals("Emergency/Trauma") && 
                                     ! String_medical_specialty.equals("NaN"))?1:0) +
           -0.01426418007575408313 * ((! String_admission_source_id.equals("Transfer from another health care facility") && 
                                     ! String_admission_type_id.equals("NaN") && 
                                     String_insulin.equals("No") && 
                                     number_diagnoses <= 7.5)?1:0) +
          -0.014128521388115843072 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_change.equals("Ch") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     number_emergency <= 0.5)?1:0) +
           0.072848709543118991827 * ((String_pioglitazone.equals("Steady"))?1:0) +
            0.03637244308885503874 * ((matchWord("infection", String_diag_1_desc))?1:0) +
         -0.0049779036287184033963 * ((matchWord("intracapsular", String_diag_1_desc))?1:0) +
          0.0013837230705762110054 * ((matchWord("asthma", String_diag_3_desc))?1:0) +
          0.0019559406350101796179 * ((matchWord("through", String_diag_2_desc))?1:0) +
         -0.0054716177633635145611 * ((matchWord("streptococcal", String_diag_2_desc))?1:0) +
          -0.023877571406786774616 * ((! String_admission_source_id.equals("Transfer from a hospital") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     number_inpatient <= 1.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
         0.00088232890767621190153 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_admission_type_id.equals("NaN") && 
                                     time_in_hospital <= 8.5 && 
                                     number_diagnoses > 8.5)?1:0) +
           -0.12779483829860005528 * ((matchWord("graft", String_diag_1_desc))?1:0) +
          0.0017952681185463064892 * ((matchWord("postsurgical", String_diag_3_desc))?1:0) +
           0.028868888468251582141 * ((String_diabetesMed.equals("Yes") && 
                                     String_discharge_disposition_id.equals("Discharged to home") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to ICF") && 
                                     number_diagnoses > 4.5)?1:0) +
         -0.0032646235510979289274 * ((matchWord("septicemia", String_diag_2_desc))?1:0) +
          -0.053666188969477175708 * ((String_glimepiride.equals("Steady"))?1:0) +
          -0.061769602506843893086 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     String_weight.equals("NaN") && 
                                     number_diagnoses <= 7.5)?1:0) +
          -0.012073889205600979793 * ((! String_insulin.equals("Down") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     num_procedures > 1.5)?1:0) +
        -0.00014646251961167644821 * ((matchWord("by", String_diag_3_desc))?1:0) +
           0.002823890434516899365 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     number_inpatient <= 0.5 && 
                                     number_diagnoses > 7.5)?1:0) +
         -0.0053497125904817775799 * ((String_diag_1.equals("415"))?1:0) +
            0.05275992051949837669 * ((matchWord("asthma", String_diag_2_desc))?1:0) +
          0.0066431000355142433289 * ((String_diag_1.equals("410"))?1:0) +
          -0.017302866877621666392 * ((String_diag_1.equals("558"))?1:0) +
          -0.050420753100379342437 * ((String_rosiglitazone.equals("Up"))?1:0) +
           0.033353524473880180601 * ((matchWord("ulcer", String_diag_2_desc))?1:0) +
         -0.0040934086241959924055 * ((matchWord("vomitus", String_diag_1_desc))?1:0) +
         -0.0033673759035443167655 * ((String_diag_2.equals("413"))?1:0) +
           0.089775317854520683691 * ((matchWord("native", String_diag_3_desc))?1:0) +
            0.18725012015719702529 * ((String_diag_2.equals("780"))?1:0) +
        -0.00015420647007779021784 * ((String_diag_3.equals("41"))?1:0) +
         -0.0017781533763957617627 * ((matchWord("for", String_diag_3_desc))?1:0) +
          0.0063503540385097768592 * ((String_diabetesMed.equals("Yes") && 
                                     String_medical_specialty.equals("NaN") && 
                                     String_race.equals("Caucasian") && 
                                     num_procedures <= 5.5)?1:0) +
          -0.013037324102628158884 * ((matchWord("sideroblastic", String_diag_1_desc))?1:0) +
          -0.088784459455205022582 * ((String_weight.equals("NaN"))?1:0) +
             0.1148646377100346061 * ((matchWord("pancreatitis", String_diag_1_desc))?1:0) +
           0.038261213727909720861 * ((String_diag_3.equals("250.6"))?1:0) +
          -0.026773064769687864722 * ((String_pioglitazone.equals("No"))?1:0) +
            0.01785887647456564295 * ((matchWord("face", String_diag_3_desc))?1:0) +
          -0.063289212525444588087 * ((String_payer_code.equals("HM"))?1:0) +
           -0.09256255188134732137 * ((matchWord("noninfectious", String_diag_1_desc))?1:0) +
          0.0016828770281223565976 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     String_medical_specialty.equals("NaN") && 
                                     String_payer_code.equals("NaN"))?1:0) +
          -0.013181730728920393672 * ((matchWord("to", String_diag_1_desc))?1:0) +
          -0.063932476332217061388 * ((matchWord("chiari", String_diag_1_desc))?1:0) +
        -0.00070076870414014381244 * ((matchWord("secondary", String_diag_1_desc))?1:0) +
          -0.010086340879080956529 * ((matchWord("encounter", String_diag_3_desc))?1:0) +
           0.079832010979946452234 * ((String_diag_1.equals("486"))?1:0) +
         0.00023005776265126970208 * ((matchWord("episode", String_diag_1_desc))?1:0) +
          -0.066426715363148911009 * ((String_medical_specialty.equals("Cardiology"))?1:0) +
            0.06060481336690526355 * ((String_max_glu_serum.equals("Norm"))?1:0) +
            0.14979200671758094443 * ((matchWord("stage", String_diag_3_desc))?1:0) +
           -0.04816471122368587432 * ((matchWord("prostate", String_diag_1_desc))?1:0) +
           0.040300551058847375319 * ((matchWord("consciousness", String_diag_2_desc))?1:0) +
         -0.0032807924140821069676 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_age.equals("[80-90)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_race.equals("Caucasian"))?1:0) +
             0.1291795043638306606 * ((String_diag_3.equals("424"))?1:0) +
            0.21089964249059783841 * ((String_diag_3.equals("427"))?1:0) +
         -0.0099459786719990461029 * ((String_diag_3.equals("428"))?1:0) +
        -0.00063304900493357080769 * ((matchWord("radiotherapy", String_diag_2_desc))?1:0) +
          -0.029147480327930672117 * ((String_gender.equals("Male"))?1:0) +
            0.37064733827599960492 * (number_inpatient) +
          -0.015298860586417323618 * ((! String_age.equals("[80-90)") && 
                                     ! String_race.equals("Caucasian") && 
                                     num_medications > 5.5 && 
                                     number_outpatient <= 0.5)?1:0) +
          -0.005502036755192467668 * ((matchWord("septicemia", String_diag_1_desc))?1:0) +
          -0.027785508854631154468 * ((! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     number_emergency <= 2.5 && 
                                     number_inpatient <= 3.5)?1:0) +
            0.01743711633628110616 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_medical_specialty.equals("ObstetricsandGynecology") && 
                                     ! String_payer_code.equals("BC"))?1:0) +
           -0.18441053614734148614 * ((String_discharge_disposition_id.equals("Hospice / home"))?1:0) +
          -0.097547032954815648953 * ((String_glipizide.equals("Down"))?1:0) +
          0.0027864868455182018048 * ((matchWord("disorders", String_diag_1_desc))?1:0) +
         -0.0018569390544140218415 * ((matchWord("fatty", String_diag_1_desc))?1:0) +
            0.05800536979112639463 * ((matchWord("supraventricular", String_diag_1_desc))?1:0) +
        -0.00091415552764804476876 * ((String_diag_3.equals("V58"))?1:0) +
         -0.0021823965537981055395 * ((matchWord("type", String_diag_1_desc))?1:0) +
             0.2475209670530650885 * (number_emergency) +
          -0.014425811017180283427 * ((! String_age.equals("[80-90)") && 
                                     String_weight.equals("NaN") && 
                                     number_emergency <= 0.5 && 
                                     number_inpatient <= 0.5)?1:0) +
         -0.0061855735763926165613 * ((matchWord("fracture", String_diag_1_desc))?1:0) +
          0.0023713725665686544684 * ((matchWord("organism", String_diag_1_desc))?1:0) +
           0.097313855440014224163 * ((String_diag_3.equals("250.02"))?1:0) +
           0.087114026199286057062 * ((String_diag_3.equals("250.01"))?1:0) +
          -0.014474312511117382388 * ((String_race.equals("AfricanAmerican") && 
                                     number_inpatient <= 0.5)?1:0) +
          0.0092916532666545391395 * ((String_diabetesMed.equals("Yes") && 
                                     String_race.equals("Caucasian") && 
                                     num_procedures <= 0.5 && 
                                     number_diagnoses > 5.5)?1:0) +
          -0.062933312273583480456 * ((String_medical_specialty.equals("Nephrology"))?1:0) +
            0.04018162199268695417 * ((String_diag_2.equals("584"))?1:0) +
         -0.0019320412922950011472 * ((matchWord("closed", String_diag_1_desc))?1:0) +
           0.086797171975969492075 * ((! String_admission_type_id.equals("Not Available") && 
                                     ! String_medical_specialty.equals("Cardiology") && 
                                     ! String_race.equals("AfricanAmerican") && 
                                     num_medications > 4.5)?1:0) +
          -0.051937075231224948124 * ((! String_medical_specialty.equals("Emergency/Trauma") && 
                                     ! String_medical_specialty.equals("InternalMedicine") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     number_outpatient <= 1.5)?1:0) +
          -0.034196646015621362524 * ((String_diag_2.equals("518"))?1:0) +
          0.0013680214398210939052 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     String_payer_code.equals("NaN") && 
                                     ! String_race.equals("AfricanAmerican") && 
                                     number_outpatient <= 5.5)?1:0) +
           0.066811475935413117133 * ((String_diag_2.equals("599"))?1:0) +
            0.18218998747920547854 * ((String_admission_type_id.equals("NaN"))?1:0) +
            0.14998664772595052264 * ((String_diag_1.equals("440"))?1:0) +
           0.010986747760890428621 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_admission_type_id.equals("Not Available") && 
                                     time_in_hospital <= 8.5 && 
                                     number_diagnoses > 8.5)?1:0) +
          0.0072163267845428476915 * ((matchWord("paroxysmal", String_diag_1_desc))?1:0) +
          0.0086832044774774810869 * ((String_diag_1.equals("562"))?1:0) +
           0.023671347905199077721 * ((matchWord("hypothyroidism", String_diag_3_desc))?1:0) +
         -0.0051987274313292693856 * ((String_glyburide_metformin.equals("No"))?1:0) +
            0.07081285429288242117 * (number_outpatient) +
           -0.14441033096015953863 * ((String_A1Cresult.equals("Norm"))?1:0) +
          -0.013821027059423422398 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     number_diagnoses <= 7.5)?1:0) +
           0.021583518134186123472 * ((! String_age.equals("[30-40)") && 
                                     String_payer_code.equals("NaN") && 
                                     num_procedures <= 1.5 && 
                                     number_diagnoses > 3.5)?1:0) +
          0.0094882682082932795486 * ((! String_discharge_disposition_id.equals("Hospice / home") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     String_medical_specialty.equals("NaN") && 
                                     number_inpatient <= 4.5)?1:0) +
          -0.036279914160712245508 * ((! String_diabetesMed.equals("Yes") && 
                                     number_outpatient <= 3.5)?1:0) +
         9.2167823781121198715E-05 * (num_lab_procedures) +
          0.0092880004327244147094 * ((! String_admission_source_id.equals("Transfer from a Skilled Nursing Facility (SNF)") && 
                                     ! String_payer_code.equals("UN") && 
                                     ! String_race.equals("AfricanAmerican") && 
                                     number_outpatient <= 5.5)?1:0) +
          -0.062336588264127371084 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     String_weight.equals("NaN") && 
                                     number_inpatient <= 2.5)?1:0) +
          -0.041083462100228000657 * ((String_diag_1.equals("250.13"))?1:0) +
          -0.080185779226928849406 * ((String_diag_1.equals("250.11"))?1:0) +
         -0.0034472707010806910057 * ((matchWord("generalized", String_diag_3_desc))?1:0) +
           0.003046926697425829042 * ((matchWord("pressure", String_diag_2_desc))?1:0) +
           0.038666705824201161312 * ((matchWord("coronary", String_diag_3_desc))?1:0) +
           -0.13290106807776197462 * ((String_diag_1.equals("250.82"))?1:0) +
          -0.018188641277188503237 * ((matchWord("liver", String_diag_3_desc))?1:0) +
         -0.0065504634306773560498 * ((! String_medical_specialty.equals("NaN") && 
                                     ! String_race.equals("Caucasian") && 
                                     num_lab_procedures <= 56.5 && 
                                     number_emergency <= 1.5)?1:0) +
         -0.0026069279728774246568 * ((matchWord("postoperative", String_diag_2_desc))?1:0) +
          0.0010091855543441245399 * ((matchWord("single", String_diag_1_desc))?1:0) +
           -0.22612308221220348003 * ((String_discharge_disposition_id.equals("small_count"))?1:0) +
           0.020651555521116932851 * ((! String_discharge_disposition_id.equals("Expired") && 
                                     num_procedures <= 0.5 && 
                                     number_emergency <= 0.5 && 
                                     number_diagnoses > 5.5)?1:0) +
           0.033881675966898915087 * ((number_inpatient > 0.5 && 
                                     number_diagnoses > 6.5)?1:0) +
          -0.023907141647763273384 * ((String_chlorpropamide.equals("No"))?1:0) +
           -0.19366517570548877347 * ((String_diag_1.equals("small_count"))?1:0) +
           0.038611592168367681044 * ((matchWord("wall", String_diag_1_desc))?1:0) +
           0.079497222679105583598 * ((! String_admission_type_id.equals("Emergency") && 
                                     ! String_admission_type_id.equals("Not Available") && 
                                     ! String_medical_specialty.equals("ObstetricsandGynecology") && 
                                     num_procedures <= 4.5)?1:0) +
           -0.18913754326412027251 * ((String_medical_specialty.equals("Orthopedics-Reconstructive"))?1:0) +
          0.0018857587931703292949 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_admission_source_id.equals("Transfer from a hospital") && 
                                     ! String_admission_source_id.equals("Transfer from another health care facility") && 
                                     num_procedures <= 0.5)?1:0) +
           0.033389142090391558948 * ((! String_age.equals("[90-100)") && 
                                     num_procedures <= 4.5 && 
                                     number_emergency <= 2.5 && 
                                     number_diagnoses > 4.5)?1:0) +
          0.0068289996711919020642 * ((! String_age.equals("[30-40)") && 
                                     ! String_race.equals("AfricanAmerican") && 
                                     num_procedures <= 0.5 && 
                                     number_emergency <= 0.5)?1:0) +
           0.036617588530025300952 * ((String_insulin.equals("Down"))?1:0) +
          -0.039724264983428236864 * ((matchWord("infarction", String_diag_2_desc))?1:0) +
           0.083849617453899516484 * ((matchWord("airway", String_diag_3_desc))?1:0) +
           0.093482269984162494336 * ((String_diag_1.equals("276"))?1:0) +
           0.010301617231253166682 * (number_diagnoses) +
          0.0023446801119111977847 * ((matchWord("neurological", String_diag_3_desc))?1:0) +
            0.20851937569590620059 * ((matchWord("circulatory", String_diag_1_desc))?1:0) +
          -0.059088318764697521368 * ((String_diag_3.equals("small_count"))?1:0) +
           -0.59845600265394482964 * ((String_discharge_disposition_id.equals("Expired"))?1:0) +
           -0.03489056198102342965 * ((! String_medical_specialty.equals("InternalMedicine") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     String_weight.equals("NaN") && 
                                     number_inpatient <= 1.5)?1:0) +
          -0.061969557448887442608 * ((String_diag_1.equals("402"))?1:0) +
           0.098030355603263411024 * ((String_diag_2.equals("428"))?1:0) +
           0.035755058962082912621 * ((String_glipizide.equals("Steady"))?1:0) +
          -0.018530042600527346641 * ((num_procedures <= 3.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
           0.068824312497647838205 * ((String_diag_2.equals("424"))?1:0) +
            0.17881736696639261019 * ((String_diag_2.equals("425"))?1:0) +
        -2.6662651399470408325E-05 * ((String_diag_2.equals("427"))?1:0) +
         0.00096082487956433856001 * ((matchWord("extrinsic", String_diag_2_desc))?1:0) +
           0.057477058039148480606 * ((matchWord("stage", String_diag_2_desc))?1:0) +
          -0.010589872810506372261 * ((String_diag_1.equals("530"))?1:0) +
          -0.010901476172435036957 * ((String_diag_1.equals("535"))?1:0) +
          -0.028239719649493556092 * ((String_repaglinide.equals("No"))?1:0) +
          -0.011496410385360950057 * ((! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     number_outpatient <= 1.5 && 
                                     number_emergency <= 2.5)?1:0) +
            -0.0423332546355139408 * ((matchWord("complication", String_diag_1_desc))?1:0) +
         0.00090246073168689109126 * ((matchWord("vessel", String_diag_3_desc))?1:0) +
          -0.000456544854233426208 * ((! String_age.equals("[80-90)") && 
                                     number_outpatient <= 0.5 && 
                                     number_diagnoses <= 5.5)?1:0) +
           0.082326508634868234005 * ((String_discharge_disposition_id.equals("Discharged/transferred to home with home health service"))?1:0) +
          -0.045731156236977058005 * ((matchWord("in", String_diag_3_desc))?1:0) +
         0.00011566477746109621652 * ((String_diag_3.equals("414"))?1:0) +
          -0.041644059625847489048 * ((String_medical_specialty.equals("ObstetricsandGynecology"))?1:0) +
           -0.06219506040070373748 * ((! String_admission_type_id.equals("NaN") && 
                                     String_race.equals("AfricanAmerican") && 
                                     number_emergency <= 0.5)?1:0) +
          -0.022162265467584645051 * ((String_diag_3.equals("250"))?1:0) +
          0.0083619986593639391431 * ((! String_discharge_disposition_id.equals("Hospice / home") && 
                                     String_medical_specialty.equals("NaN") && 
                                     num_medications > 10.5 && 
                                     number_outpatient <= 4.5)?1:0) +
            0.14967208141070187377 * ((String_diag_3.equals("707"))?1:0) +
        -0.00089497298993523616978 * ((String_metformin.equals("No"))?1:0) +
            0.12285937779647054802 * ((String_diag_1.equals("584"))?1:0) +
         0.00068613227661237156767 * ((String_race.equals("Hispanic"))?1:0) +
          -0.029263180052309028384 * ((! String_glipizide.equals("Steady") && 
                                     ! String_medical_specialty.equals("InternalMedicine") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     number_inpatient <= 1.5)?1:0) +
          -0.088927135356344583217 * ((String_age.equals("[30-40)"))?1:0) +
          0.0018961086739130504457 * ((matchWord("hyperosmolality", String_diag_3_desc))?1:0) +
        -5.5245895250564928014E-05 * ((String_diag_3.equals("V45"))?1:0) +
           0.018723269551332716309 * ((String_weight.equals("NaN") && 
                                     num_procedures <= 1.5 && 
                                     number_diagnoses > 7.5)?1:0) +
         0.00012726002381141252929 * ((matchWord("supraventricular", String_diag_3_desc))?1:0) +
         -0.0039147549089319291782 * ((String_discharge_disposition_id.equals("Discharged/transferred to a long term care hospital."))?1:0) +
          -0.011456969914480044648 * ((! String_admission_type_id.equals("Elective") && 
                                     ! String_payer_code.equals("CP") && 
                                     ! String_payer_code.equals("NaN") && 
                                     ! String_weight.equals("[75-100)"))?1:0) +
           0.003416117818751985679 * ((String_payer_code.equals("OG"))?1:0) +
           0.001656983699354573061 * ((matchWord("peripheral", String_diag_1_desc))?1:0) +
           0.009223083905158713583 * ((matchWord("site", String_diag_2_desc))?1:0) +
           0.029727089546981892015 * ((! String_admission_type_id.equals("Emergency") && 
                                     ! String_age.equals("[90-100)") && 
                                     ! String_medical_specialty.equals("ObstetricsandGynecology") && 
                                     num_medications <= 46.5)?1:0) +
          -0.082090917402215274334 * ((String_diag_1.equals("250.4"))?1:0) +
            0.13030186571015214825 * ((String_diag_1.equals("250.7"))?1:0) +
         -0.0057830965327698357833 * ((String_diag_2.equals("V45"))?1:0) +
          0.0025419053549657864843 * ((matchWord("extrinsic", String_diag_3_desc))?1:0) +
        -0.00048569517912983083217 * ((String_admission_source_id.equals("Emergency Room") && 
                                     ! String_admission_type_id.equals("NaN") && 
                                     number_emergency <= 0.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
           0.063698131719597192446 * ((String_diag_1.equals("250.8"))?1:0) +
            0.24715277931631340902 * ((matchWord("osteoarthrosis", String_diag_1_desc))?1:0) +
          -0.036664028324950914683 * ((! String_admission_type_id.equals("Elective") && 
                                     ! String_age.equals("[30-40)") && 
                                     num_lab_procedures <= 64.5 && 
                                     number_emergency <= 0.5)?1:0) +
           0.047395346311860708788 * ((matchWord("hyperosmolality", String_diag_1_desc))?1:0) +
          0.0014754749299068677104 * ((matchWord("manic", String_diag_1_desc))?1:0) +
           0.048741194276835812793 * ((matchWord("urinary", String_diag_3_desc))?1:0) +
          -0.087015153627253552515 * ((String_admission_source_id.equals("Transfer from a Skilled Nursing Facility (SNF)"))?1:0) +
          0.0031005746726722230831 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     time_in_hospital <= 5.5 && 
                                     number_diagnoses > 7.5)?1:0) +
         -0.0022335265683439313802 * ((String_age.equals("[40-50)"))?1:0) +
            0.06911251615127562975 * ((String_diag_2.equals("486"))?1:0) +
           -0.00904809029945474394 * ((num_procedures <= 3.5 && 
                                     number_emergency <= 0.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
           0.022106893177759618513 * ((String_discharge_disposition_id.equals("Left AMA"))?1:0) +
           0.056726627063845555532 * ((String_diag_1.equals("574"))?1:0) +
             0.1461910762358986926 * ((String_diag_1.equals("577"))?1:0) +
           -0.17376555045684330403 * ((String_diag_1.equals("571"))?1:0) +
          0.0042736487820445685434 * ((matchWord("graft", String_diag_2_desc))?1:0) +
           -0.15540839711304058057 * ((String_diag_1.equals("578"))?1:0) +
          -0.034097559860544128518 * ((String_diag_2.equals("small_count"))?1:0) +
           -0.12476297879302050697 * ((matchWord("appendicitis", String_diag_1_desc))?1:0) +
         -0.0071697895417563266285 * ((matchWord("pneumonitis", String_diag_1_desc))?1:0) +
          -0.096093264787243837244 * ((String_diag_1.equals("185"))?1:0) +
          -0.018179040134436191822 * ((! String_admission_type_id.equals("Elective") && 
                                     ! String_payer_code.equals("CP") && 
                                     ! String_payer_code.equals("NaN") && 
                                     number_outpatient <= 1.5)?1:0) +
           0.053332745626885828816 * ((matchWord("ulcer", String_diag_3_desc))?1:0) +
          0.0017241531302333297331 * ((String_diabetesMed.equals("Yes"))?1:0) +
           -0.35831734744661591918 * ((String_admission_source_id.equals("Transfer from a hospital"))?1:0) ;
    }
    private double score2(Map row) {
        String String_rosiglitazone = (String)row.get("rosiglitazone");
        String String_medical_specialty = (String)row.get("medical_specialty");
        float number_diagnoses = colAsNumeric(row, "number_diagnoses");
        float num_medications = colAsNumeric(row, "num_medications");
        String String_pioglitazone = (String)row.get("pioglitazone");
        float number_inpatient = colAsNumeric(row, "number_inpatient");
        String String_race = (String)row.get("race");
        String String_discharge_disposition_id = (String)row.get("discharge_disposition_id");
        String String_weight = (String)row.get("weight");
        String String_glyburide = (String)row.get("glyburide");
        String String_glimepiride = (String)row.get("glimepiride");
        String String_repaglinide = (String)row.get("repaglinide");
        String String_diag_3 = (String)row.get("diag_3");
        String String_diag_2 = (String)row.get("diag_2");
        String String_diag_1 = (String)row.get("diag_1");
        String String_admission_source_id = (String)row.get("admission_source_id");
        String String_insulin = (String)row.get("insulin");
        String String_diag_1_desc = (String)row.get("diag_1_desc");
        String String_A1Cresult = (String)row.get("A1Cresult");
        String String_glipizide = (String)row.get("glipizide");
        String String_diabetesMed = (String)row.get("diabetesMed");
        String String_acarbose = (String)row.get("acarbose");
        String String_payer_code = (String)row.get("payer_code");
        float num_lab_procedures = colAsNumeric(row, "num_lab_procedures");
        String String_admission_type_id = (String)row.get("admission_type_id");
        String String_age = (String)row.get("age");
        String String_diag_3_desc = (String)row.get("diag_3_desc");
        float number_emergency = colAsNumeric(row, "number_emergency");
        String String_max_glu_serum = (String)row.get("max_glu_serum");
        float number_outpatient = colAsNumeric(row, "number_outpatient");
        String String_diag_2_desc = (String)row.get("diag_2_desc");
        float num_procedures = colAsNumeric(row, "num_procedures");
        String String_metformin = (String)row.get("metformin");
        return
          -0.076363141067437440013 * ((matchWord("asthma", String_diag_1_desc))?1:0) +
           -0.06305387767693085288 * ((String_diag_2.equals("250"))?1:0) +
          -0.016414956918077108772 * ((matchWord("pleurisy", String_diag_1_desc))?1:0) +
            0.23978796511647529344 * ((String_payer_code.equals("CP"))?1:0) +
            0.14867185044735764521 * ((String_diag_1.equals("786"))?1:0) +
           -0.13420374258073361484 * ((String_diag_1.equals("250.22"))?1:0) +
           0.012389751182281709754 * ((matchWord("chronic", String_diag_3_desc))?1:0) +
            0.14091704891038051017 * ((String_diag_2.equals("682"))?1:0) +
          -0.012892758126639808705 * ((matchWord("extrinsic", String_diag_1_desc))?1:0) +
           0.011877553296838580521 * ((matchWord("alteration", String_diag_3_desc))?1:0) +
        -0.00040489041906204573324 * ((String_diag_1.equals("295"))?1:0) +
           0.063959337704195248775 * ((matchWord("infarction", String_diag_1_desc))?1:0) +
        -6.1378904913585830656E-05 * ((matchWord("renal", String_diag_1_desc))?1:0) +
          0.0013403361382234603699 * ((! String_age.equals("[80-90)") && 
                                     String_race.equals("Caucasian") && 
                                     num_procedures <= 4.5 && 
                                     num_medications > 5.5)?1:0) +
          -0.018500779903240022889 * ((String_diag_1.equals("820"))?1:0) +
           -0.16737196164221404548 * ((matchWord("achalasia", String_diag_1_desc))?1:0) +
           0.041363052932079057145 * ((matchWord("gallbladder", String_diag_1_desc))?1:0) +
          0.0011871553560767779401 * ((! String_admission_source_id.equals("Transfer from a hospital") && 
                                     ! String_admission_source_id.equals("Transfer from another health care facility") && 
                                     String_diabetesMed.equals("Yes") && 
                                     number_inpatient <= 1.5)?1:0) +
         -0.0087036514675084830639 * ((! String_payer_code.equals("NaN") && 
                                     ! String_weight.equals("[75-100)") && 
                                     num_procedures > 0.5 && 
                                     number_inpatient <= 0.5)?1:0) +
            0.18921654943899726686 * ((String_diag_3.equals("599"))?1:0) +
          -0.010976305555099581321 * ((! String_admission_type_id.equals("NaN") && 
                                     num_procedures > 1.5 && 
                                     number_inpatient <= 0.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
         -0.0061196283798005913709 * ((! String_payer_code.equals("NaN") && 
                                     String_weight.equals("NaN") && 
                                     num_procedures > 0.5 && 
                                     number_diagnoses > 3.5)?1:0) +
           -0.16215169754909344868 * ((matchWord("lung", String_diag_1_desc))?1:0) +
        -0.00038390020333548783172 * ((matchWord("failure", String_diag_3_desc))?1:0) +
          -0.011501251030772508335 * ((! String_discharge_disposition_id.equals("Discharged to home") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     ! String_payer_code.equals("BC") && 
                                     number_inpatient <= 2.5)?1:0) +
           0.052455248630979584012 * ((String_pioglitazone.equals("small_count"))?1:0) +
            0.02153779164310389363 * ((String_diabetesMed.equals("Yes") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     number_diagnoses > 5.5)?1:0) +
           0.012137451291950169391 * ((String_diag_1.equals("715"))?1:0) +
          -0.021271806460845213427 * ((matchWord("malignant", String_diag_1_desc))?1:0) +
        -0.00011724159196664120375 * ((! String_pioglitazone.equals("Steady") && 
                                     String_race.equals("AfricanAmerican") && 
                                     num_lab_procedures <= 52.5 && 
                                     number_outpatient <= 0.5)?1:0) +
           0.017090315654971658527 * ((String_age.equals("[60-70)"))?1:0) +
          -0.031376867978531335079 * ((matchWord("juvenile", String_diag_1_desc))?1:0) +
          -0.011222473402657097072 * ((matchWord("streptococcal", String_diag_1_desc))?1:0) +
          -0.037556940412228789761 * ((String_rosiglitazone.equals("small_count"))?1:0) +
           0.019175286464160282845 * ((matchWord("valve", String_diag_2_desc))?1:0) +
         -0.0024589080153168962894 * ((! String_insulin.equals("Down") && 
                                     ! String_medical_specialty.equals("NaN") && 
                                     num_lab_procedures <= 35.5 && 
                                     number_emergency <= 0.5)?1:0) +
         -0.0011161746989684928361 * ((matchWord("angina", String_diag_2_desc))?1:0) +
           -0.01028657146142293588 * ((matchWord("collapse", String_diag_2_desc))?1:0) +
         -0.0059957755253971407872 * ((String_glyburide.equals("Down"))?1:0) +
          -0.041527071572293836055 * ((matchWord("for", String_diag_2_desc))?1:0) +
          -0.016515073168353891497 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     num_procedures > 0.5 && 
                                     number_diagnoses <= 8.5)?1:0) +
          -0.058537319427728974619 * ((String_medical_specialty.equals("Urology"))?1:0) +
          0.0017137643306631665518 * ((! String_payer_code.equals("CP") && 
                                     String_payer_code.equals("NaN") && 
                                     num_lab_procedures > 35.5 && 
                                     number_diagnoses > 3.5)?1:0) +
         -0.0039908519759330471702 * ((matchWord("hematemesis", String_diag_1_desc))?1:0) +
          0.0026032835000843563764 * ((! String_A1Cresult.equals("Norm") && 
                                     ! String_age.equals("[30-40)") && 
                                     String_medical_specialty.equals("NaN") && 
                                     number_emergency <= 1.5)?1:0) +
           -0.12984858335879576052 * ((String_metformin.equals("Up"))?1:0) +
         -0.0044948695781234808064 * ((! String_medical_specialty.equals("NaN") && 
                                     num_lab_procedures <= 56.5)?1:0) +
         -0.0012680886081760029457 * ((String_diag_1.equals("507"))?1:0) +
          -0.031789417891698076857 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_diabetesMed.equals("Yes") && 
                                     number_outpatient <= 1.5)?1:0) +
         0.00034430184287065427598 * ((! String_glipizide.equals("Up") && 
                                     ! String_payer_code.equals("UN") && 
                                     num_lab_procedures > 35.5 && 
                                     number_emergency <= 0.5)?1:0) +
         -0.0024711520156120748981 * ((matchWord("osteoarthrosis", String_diag_3_desc))?1:0) +
          -0.037514089729282919239 * ((matchWord("ii", String_diag_2_desc))?1:0) +
          -0.051861045442473580058 * ((String_diag_2.equals("285"))?1:0) +
           0.053570043370709433117 * ((String_diag_3.equals("780"))?1:0) +
           0.034853546079158873039 * ((! String_age.equals("[90-100)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     ! String_payer_code.equals("BC"))?1:0) +
            0.25112789503198312824 * ((String_weight.equals("[75-100)"))?1:0) +
           0.036250611801223443786 * ((String_weight.equals("small_count"))?1:0) +
           0.029569877263955705871 * ((String_metformin.equals("Steady"))?1:0) +
          -0.014209488794544745255 * ((String_age.equals("[50-60)"))?1:0) +
          -0.044803642789338279928 * ((String_diag_1.equals("38"))?1:0) +
            0.10729126864215922377 * ((String_repaglinide.equals("Steady"))?1:0) +
          -0.005873209357093902086 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_medical_specialty.equals("Cardiology") && 
                                     num_procedures > 1.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
         0.00026768449140568916465 * ((String_diag_3.equals("403"))?1:0) +
        -0.00081250385451726370774 * ((matchWord("section", String_diag_1_desc))?1:0) +
         -0.0033436155101614485445 * ((String_diag_3.equals("401"))?1:0) +
         -0.0028530359469900504719 * ((String_diag_3.equals("715"))?1:0) +
           0.010304541719337315803 * ((matchWord("myocardial", String_diag_1_desc))?1:0) +
          -0.026606189088976609641 * (num_procedures) +
          -0.033599352084286535081 * ((String_discharge_disposition_id.equals("Not Mapped"))?1:0) +
           0.066575168813648527566 * ((String_diag_3.equals("496"))?1:0) +
           0.018695002452365983708 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     String_discharge_disposition_id.equals("Discharged to home") && 
                                     ! String_glipizide.equals("Down") && 
                                     number_diagnoses > 4.5)?1:0) +
           0.072072017177593150628 * ((String_diag_3.equals("493"))?1:0) +
          -0.023363711783294553032 * ((! String_discharge_disposition_id.equals("Discharged/transferred to ICF") && 
                                     ! String_glipizide.equals("Steady") && 
                                     ! String_medical_specialty.equals("Endocrinology") && 
                                     String_race.equals("AfricanAmerican"))?1:0) +
            0.10247360250953525818 * ((String_diag_1.equals("599"))?1:0) +
          -0.069288173860556148687 * ((! String_discharge_disposition_id.equals("Discharged to home") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to another rehab fac including rehab units of a hospital.") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     String_repaglinide.equals("No"))?1:0) +
           -0.14336417334046469563 * ((matchWord("osteoporosis", String_diag_1_desc))?1:0) +
           0.068850816991789742794 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_admission_source_id.equals("Transfer from a hospital") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     String_payer_code.equals("NaN"))?1:0) +
          -0.011020994614777049683 * ((! String_diabetesMed.equals("Yes") && 
                                     number_inpatient <= 0.5)?1:0) +
          0.0074511674796660090289 * ((String_race.equals("Caucasian"))?1:0) +
          0.0029116558749819115501 * ((matchWord("site", String_diag_1_desc))?1:0) +
         -0.0076122739349373368806 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     number_emergency <= 0.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
             0.1559135350453891844 * ((! String_weight.equals("NaN"))?1:0) +
          0.0092258445458985004206 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     ! String_payer_code.equals("CP") && 
                                     String_payer_code.equals("NaN"))?1:0) +
          -0.076432349698257309734 * ((String_race.equals("AfricanAmerican"))?1:0) +
          -0.071476436508604718201 * ((matchWord("sideroblastic", String_diag_2_desc))?1:0) +
          -0.035422450684636057339 * ((! String_admission_type_id.equals("Not Available") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     String_max_glu_serum.equals("NaN") && 
                                     number_diagnoses <= 8.5)?1:0) +
           0.035069123651444294998 * ((! String_discharge_disposition_id.equals("Hospice / home") && 
                                     ! String_discharge_disposition_id.equals("NaN") && 
                                     String_medical_specialty.equals("NaN"))?1:0) +
          -0.028692354494384526148 * ((String_age.equals("[20-30)"))?1:0) +
          -0.053298580481697076239 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_medical_specialty.equals("Emergency/Trauma") && 
                                     String_rosiglitazone.equals("No") && 
                                     number_diagnoses <= 8.5)?1:0) +
           -0.12255513639708613682 * ((String_admission_source_id.equals("small_count"))?1:0) +
        -6.5466264082246467372E-05 * ((matchWord("situ", String_diag_3_desc))?1:0) +
          -0.019897979056145188859 * ((matchWord("anemia", String_diag_2_desc))?1:0) +
         -0.0012281296940206036863 * ((String_diag_2.equals("V58"))?1:0) +
          0.0054658901343902889861 * ((matchWord("hypertension", String_diag_2_desc))?1:0) +
          -0.027784338365402380666 * ((matchWord("decubitus", String_diag_2_desc))?1:0) +
           -0.24066922866591183849 * ((String_admission_source_id.equals("Clinic Referral"))?1:0) +
           -0.14158417726033181716 * ((matchWord("paroxysmal", String_diag_2_desc))?1:0) +
          -0.048257288164187769897 * ((matchWord("nervous", String_diag_1_desc))?1:0) +
           -0.03129844583981342826 * ((matchWord("streptococcus", String_diag_3_desc))?1:0) +
           0.017130291772187018068 * ((matchWord("cholecystitis", String_diag_1_desc))?1:0) +
           0.034614420641508880649 * ((String_diabetesMed.equals("Yes") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to ICF") && 
                                     number_inpatient <= 3.5 && 
                                     number_diagnoses > 4.5)?1:0) +
          -0.013641345145213444601 * ((matchWord("pulmonary", String_diag_2_desc))?1:0) +
          -0.036605046462424309373 * ((! String_age.equals("[80-90)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     String_weight.equals("NaN") && 
                                     number_inpatient <= 1.5)?1:0) +
            0.12514149394926649128 * ((String_diag_2.equals("491"))?1:0) +
           -0.15426458082530053462 * ((String_admission_source_id.equals("Transfer from another health care facility"))?1:0) +
          0.0020336462839237300776 * ((String_diag_2.equals("493"))?1:0) +
          -0.054224530732067274807 * ((matchWord("ketoacidosis", String_diag_1_desc))?1:0) +
         -0.0085803307086413233007 * ((String_diag_1.equals("540"))?1:0) +
            0.03858609110013851845 * ((String_glyburide.equals("Up"))?1:0) +
        -0.00056232305721692172826 * ((matchWord("budd", String_diag_1_desc))?1:0) +
          -0.034769911755362967187 * ((! String_admission_type_id.equals("NaN") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service") && 
                                     number_inpatient <= 0.5 && 
                                     number_diagnoses <= 7.5)?1:0) +
           0.026838367584610171163 * ((String_diag_2.equals("403"))?1:0) +
           -0.16228098762038323244 * ((String_payer_code.equals("UN"))?1:0) +
           0.085254577990618182759 * ((matchWord("hypernatremia", String_diag_2_desc))?1:0) +
           0.055282690146592726455 * ((matchWord("congestive", String_diag_2_desc))?1:0) +
         0.00081335888978288682985 * ((String_glimepiride.equals("No"))?1:0) +
            0.21647533213314359979 * ((String_diag_2.equals("250.01"))?1:0) +
            0.11532120873954730678 * ((matchWord("cellulitis", String_diag_1_desc))?1:0) +
         -0.0022025351339628657966 * ((! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_medical_specialty.equals("ObstetricsandGynecology") && 
                                     String_weight.equals("NaN") && 
                                     num_lab_procedures <= 63.5)?1:0) +
           0.034270820769184542898 * ((String_medical_specialty.equals("Orthopedics"))?1:0) +
          -0.085690029377305590441 * ((matchWord("essential", String_diag_3_desc))?1:0) +
           -0.16690668951994702685 * ((String_glipizide.equals("Up"))?1:0) +
            0.13507784188367544242 * ((matchWord("endomyocardial", String_diag_2_desc))?1:0) +
          -0.072063034854348909097 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     ! String_admission_type_id.equals("Elective") && 
                                     num_procedures <= 5.5 && 
                                     number_emergency <= 0.5)?1:0) +
           0.026540651161228862032 * ((matchWord("bipolar", String_diag_1_desc))?1:0) +
           -0.06152834828790543914 * ((! String_payer_code.equals("UN") && 
                                     ! String_rosiglitazone.equals("Steady") && 
                                     String_weight.equals("NaN") && 
                                     number_inpatient <= 3.5)?1:0) +
          -0.073030956789463058465 * ((String_diag_1.equals("285"))?1:0) +
          -0.011625062491025394198 * ((String_diag_2.equals("38"))?1:0) +
           0.011729813878686616915 * ((String_race.equals("Caucasian") && 
                                     num_procedures <= 1.5 && 
                                     num_medications > 5.5 && 
                                     number_diagnoses > 8.5)?1:0) +
          -0.036833495789466201564 * ((String_diag_1.equals("997"))?1:0) +
           0.077328873962847613499 * ((! String_age.equals("[90-100)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     num_procedures <= 2.5 && 
                                     number_diagnoses > 3.5)?1:0) +
         -0.0056375006336395914672 * ((String_diag_1.equals("493"))?1:0) +
            0.10146349045977731684 * ((String_diag_1.equals("491"))?1:0) +
           -0.10551352432573658791 * ((String_diag_1.equals("724"))?1:0) +
            0.02055564551311153329 * ((matchWord("failure", String_diag_2_desc))?1:0) +
           0.029242535587899812222 * ((matchWord("mitral", String_diag_2_desc))?1:0) +
          -0.028274629747955443149 * ((String_acarbose.equals("No"))?1:0) +
          0.0017716835555333055804 * ((matchWord("pressure", String_diag_3_desc))?1:0) +
          -0.047098351031294137525 * ((! String_age.equals("[80-90)") && 
                                     number_outpatient <= 1.5 && 
                                     number_inpatient <= 0.5 && 
                                     number_diagnoses <= 6.5)?1:0) +
             0.2487988929848650721 * ((String_weight.equals("[50-75)"))?1:0) +
           0.017564097991984075481 * ((String_age.equals("[70-80)"))?1:0) +
           -0.14917406541793559738 * ((matchWord("current", String_diag_1_desc))?1:0) +
           -0.15747596909655317554 * ((String_discharge_disposition_id.equals("Discharged/transferred to ICF"))?1:0) +
         -0.0010199764982403846259 * ((matchWord("food", String_diag_1_desc))?1:0) +
           -0.18193879707222634923 * ((matchWord("cor", String_diag_1_desc))?1:0) +
          -0.059764203995019395121 * ((matchWord("collapse", String_diag_1_desc))?1:0) +
           0.037080804707361858519 * ((matchWord("hypernatremia", String_diag_3_desc))?1:0) +
            0.13213492063227072482 * ((matchWord("hyperosmolality", String_diag_2_desc))?1:0) +
            0.23097843725849598329 * ((String_age.equals("[80-90)"))?1:0) +
           0.038511431830629966333 * ((matchWord("alteration", String_diag_2_desc))?1:0) +
          0.0081256087320488376569 * ((! String_A1Cresult.equals("Norm") && 
                                     ! String_admission_source_id.equals("Clinic Referral") && 
                                     number_outpatient <= 5.5 && 
                                     number_diagnoses > 8.5)?1:0) +
          -0.083959707726960416951 * ((String_payer_code.equals("BC"))?1:0) +
          -0.013819756285406938415 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     String_weight.equals("NaN") && 
                                     number_emergency <= 2.5 && 
                                     number_diagnoses > 4.5)?1:0) +
         -0.0071859184620322929735 * ((! String_glipizide.equals("Up") && 
                                     ! String_payer_code.equals("UN") && 
                                     num_lab_procedures <= 35.5 && 
                                     number_emergency <= 0.5)?1:0) +
          -0.018501504911071470216 * ((matchWord("replaced", String_diag_3_desc))?1:0) +
           -0.11781673654306711041 * ((matchWord("schizophrenia", String_diag_1_desc))?1:0) +
           0.049969893333570297722 * ((String_diabetesMed.equals("Yes") && 
                                     String_race.equals("Caucasian") && 
                                     num_procedures <= 5.5 && 
                                     number_emergency <= 3.5)?1:0) +
           0.044782055333016879128 * ((! String_A1Cresult.equals("Norm") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_insulin.equals("No") && 
                                     ! String_race.equals("NaN"))?1:0) +
            0.18000816577213574199 * ((String_diag_1.equals("428"))?1:0) +
             0.1058565321702112233 * ((String_diag_1.equals("427"))?1:0) +
           -0.01750265275644271451 * ((String_diag_1.equals("733"))?1:0) +
            0.01694230451415509181 * ((String_diag_2.equals("707"))?1:0) +
         -0.0078807876337157866525 * ((String_insulin.equals("Up"))?1:0) +
          -0.037205760598193915456 * ((! String_admission_source_id.equals("Clinic Referral") && 
                                     ! String_discharge_disposition_id.equals("Discharged to home") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to another rehab fac including rehab units of a hospital.") && 
                                     ! String_discharge_disposition_id.equals("Discharged/transferred to home with home health service"))?1:0) +
        -5.9027446961122968447E-05 * ((! String_age.equals("[80-90)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_payer_code.equals("CP") && 
                                     number_inpatient <= 2.5)?1:0) +
          -0.025976601313473464405 * ((! String_payer_code.equals("UN") && 
                                     String_weight.equals("NaN") && 
                                     num_lab_procedures <= 54.5 && 
                                     num_procedures > 0.5)?1:0) +
          -0.066484184992429015693 * ((! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_race.equals("NaN") && 
                                     number_emergency <= 0.5 && 
                                     number_diagnoses <= 5.5)?1:0) +
           0.016219080167674905513 * ((! String_age.equals("[30-40)") && 
                                     ! String_discharge_disposition_id.equals("Expired") && 
                                     ! String_race.equals("AfricanAmerican") && 
                                     number_emergency <= 3.5)?1:0) +
          -0.010762181935931987298 * ((String_diag_1.equals("453"))?1:0) +
        -0.00068518594936999835301 * ((String_diag_1.equals("511"))?1:0) +
          0.0084338484138090884235 * ((! String_admission_source_id.equals("Transfer from a hospital") && 
                                     ! String_admission_type_id.equals("Not Available") && 
                                     number_inpatient <= 2.5 && 
                                     number_diagnoses > 8.5)?1:0) +
          -0.030899017691175069411 * ((String_max_glu_serum.equals("NaN"))?1:0) +
          -0.041944854147424409652 * ((String_diag_1.equals("518"))?1:0) +
          -0.017490328376613226147 * ((! String_diabetesMed.equals("Yes") && 
                                     String_weight.equals("NaN") && 
                                     number_emergency <= 0.5 && 
                                     number_inpatient <= 0.5)?1:0) +
         8.9541345522687720414E-05 * ((matchWord("consciousness", String_diag_3_desc))?1:0) +
           -0.14572932875042288181 * ((matchWord("gastritis", String_diag_1_desc))?1:0) +
          0.0010008977533657468974 * ((matchWord("tachycardia", String_diag_3_desc))?1:0) +
           0.041712360505260802357 * ((String_weight.equals("[100-125)"))?1:0) +
          -0.015051896224603573959 * ((! String_admission_source_id.equals("Emergency Room") && 
                                     ! String_payer_code.equals("NaN") && 
                                     number_outpatient <= 1.5 && 
                                     number_emergency <= 2.5)?1:0) ;
    }

    private static void initTypeConvert0(Map<String, List> aMap) {
        List convert_info;
    }
    private static final Map<String, List> typeConvert;
    static {
        Map<String, List> aMap = new HashMap();
        initTypeConvert0(aMap);
        typeConvert = Collections.unmodifiableMap(aMap);
    }

    private static String[] indicatorCols = {};

   private static void initImputeValues0(Map<String, Object> aMap) {
        aMap.put("number_emergency", "0.0");
        aMap.put("time_in_hospital", "4.0");
        aMap.put("number_diagnoses", "7.0");
        aMap.put("num_lab_procedures", "45.0");
        aMap.put("number_outpatient", "0.0");
        aMap.put("num_medications", "14.0");
        aMap.put("number_inpatient", "0.0");
        aMap.put("num_procedures", "1.0");
    }
    private static final Map<String, Object> imputeValues;
    static {
        Map<String, Object> aMap = new HashMap();
        initImputeValues0(aMap);
        imputeValues = Collections.unmodifiableMap(aMap);
    }

    private static final String[] NA_VALUES = {"null", "na", "n/a", "#N/A", "N/A", "?", ".", "", "Inf", "INF", "inf", "-inf", "-Inf", "-INF", " ", "None", "NaN", "-nan", "NULL", "NA", "-1.#IND", "1.#IND", "-1.#QNAN", "1.#QNAN", "#NA", "#N/A N/A", "-NaN", "nan"};


    private static final List NA_VALUES_LIST = Arrays.asList(NA_VALUES);

    private String sanitizeName(String name) {
        String safe = name.trim().replace("-", "_").replace("$", "_").replace(".", "_")
                          .replace("{", "_").replace("}", "_").replace("\"", "_");
        return safe;
    }

    private void convertBool(Map.Entry element) {
        VarTypes me = varTypes.get((String)element.getKey());
        if (me != null && (me == VarTypes.NUMERIC)) {
            if (Boolean.parseBoolean((String)element.getValue())) {
                element.setValue("1");
            } else if (((String)element.getValue()).equalsIgnoreCase("false")) {
                element.setValue("0");
            }
        }
    }

    private Map renameColumns(Map row) {
        Map newRow = new LinkedHashMap();
        Set existingNames = new HashSet<String>();
        Iterator i = row.entrySet().iterator();
        int blank_index = 0;
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String originalName = (String)me.getKey();
            String newName = sanitizeName(originalName);
            if(newName.isEmpty()) {
                newName = String.format("Unnamed: %d", blank_index++);
            }
            if(existingNames.contains(newName)) {
                throw new IllegalArgumentException(
                    "Duplication detected. Column with name=[" + originalName
                    + "] was preprocessed to [" + newName + "] that already exists.");
            }
            existingNames.add(newName);
            newRow.put(newName, me.getValue());
        }
        return newRow;
    }

    private void addMissingIndicators(Map row) {
        for(String col:indicatorCols) {
            Object val = row.get(col);

            Boolean isMissing = (val == null || val.equals(Float.NaN) ||
                                 (!(val instanceof Float) && ((String)val).isEmpty()));

            if(isMissing) {
                row.put(col + "-mi", "1");
            } else {
                row.put(col + "-mi", "0");
            }
        }
    }

    private void imputeMissingValues(Map row) {
        Iterator i = imputeValues.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String col = (String)me.getKey();
            Object colValue = row.get(col);
            String imputeValue = (String)me.getValue();
            VarTypes varType = (VarTypes)varTypes.get(col);
            if (TRANSFORMED_FEATURES.containsKey(col) && varType == VarTypes.NUMERIC) {
                if (colValue == null || Float.isNaN((Float)colValue)) {
                    row.put(col, stringToFloat(imputeValue));
                }
            } else {
                if (colValue == null || ((String)colValue).isEmpty()) {
                    row.put(col, imputeValue);
                }
            }
        }
    }

    
    private static void initbigLevels0(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels0.init(levels);
        aMap.put("chlorpropamide", levels);
    }
   private static void initbigLevels1(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels1.init(levels);
        aMap.put("weight", levels);
    }
   private static void initbigLevels2(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels2.init(levels);
        aMap.put("repaglinide", levels);
    }
   private static void initbigLevels3(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels3.init(levels);
        aMap.put("payer_code", levels);
    }
   private static void initbigLevels4(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels4.init(levels);
        aMap.put("acarbose", levels);
    }
   private static void initbigLevels5(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels5.init(levels);
        aMap.put("rosiglitazone", levels);
    }
   private static void initbigLevels6(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels6.init(levels);
        aMap.put("glipizide", levels);
    }
   private static void initbigLevels7(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels7.init(levels);
        aMap.put("admission_source_id", levels);
    }
   private static void initbigLevels8(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels8.init(levels);
        aMap.put("glyburide", levels);
    }
   private static void initbigLevels9(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels9.init(levels);
        aMap.put("metformin", levels);
    }
   private static void initbigLevels10(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels10.init(levels);
        aMap.put("pioglitazone", levels);
    }
   private static void initbigLevels11(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels11.init(levels);
        aMap.put("glimepiride", levels);
    }
   private static void initbigLevels12(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels12.init(levels);
        aMap.put("A1Cresult", levels);
    }
   private static void initbigLevels13(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels13.init(levels);
        aMap.put("glyburide_metformin", levels);
    }
   private static void initbigLevels14(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels14.init(levels);
        aMap.put("max_glu_serum", levels);
    }
   private static void initbigLevels15(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels15.init(levels);
        aMap.put("diabetesMed", levels);
    }
   private static void initbigLevels16(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels16.init(levels);
        aMap.put("discharge_disposition_id", levels);
    }
   private static void initbigLevels17(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels17.init(levels);
        aMap.put("change", levels);
    }
   private static void initbigLevels18(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels18.init(levels);
        aMap.put("gender", levels);
    }
   private static void initbigLevels19(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels19.init(levels);
        aMap.put("age", levels);
    }
   private static void initbigLevels20(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels20.init(levels);
        aMap.put("medical_specialty", levels);
    }
   private static void initbigLevels21(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels21.init(levels);
        aMap.put("race", levels);
    }
   private static void initbigLevels22(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels22.init(levels);
        aMap.put("insulin", levels);
    }
   private static void initbigLevels23(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels23.init(levels);
        aMap.put("diag_1", levels);
    }
   private static void initbigLevels24(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels24.init(levels);
        aMap.put("diag_2", levels);
    }
   private static void initbigLevels25(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels25.init(levels);
        aMap.put("diag_3", levels);
    }
   private static void initbigLevels26(Map<String, Set> aMap) {
        Set levels;
        levels = new HashSet();
        InitBigLevels26.init(levels);
        aMap.put("admission_type_id", levels);
    }
   private static void initbigLevels27(Map<String, Set> aMap) {
        Set levels;
    }
    private static final Map<String, Set> bigLevels;
    static {
        Map<String, Set> aMap = new HashMap();
        initbigLevels0(aMap);
        initbigLevels1(aMap);
        initbigLevels2(aMap);
        initbigLevels3(aMap);
        initbigLevels4(aMap);
        initbigLevels5(aMap);
        initbigLevels6(aMap);
        initbigLevels7(aMap);
        initbigLevels8(aMap);
        initbigLevels9(aMap);
        initbigLevels10(aMap);
        initbigLevels11(aMap);
        initbigLevels12(aMap);
        initbigLevels13(aMap);
        initbigLevels14(aMap);
        initbigLevels15(aMap);
        initbigLevels16(aMap);
        initbigLevels17(aMap);
        initbigLevels18(aMap);
        initbigLevels19(aMap);
        initbigLevels20(aMap);
        initbigLevels21(aMap);
        initbigLevels22(aMap);
        initbigLevels23(aMap);
        initbigLevels24(aMap);
        initbigLevels25(aMap);
        initbigLevels26(aMap);
        initbigLevels27(aMap);
        bigLevels = Collections.unmodifiableMap(aMap);
    }


    
    private static void initsmallNulls0(Map<String, Boolean> aMap) {
        aMap.put("gender", true);
        aMap.put("age", true);
        aMap.put("metformin", true);
        aMap.put("repaglinide", true);
        aMap.put("chlorpropamide", true);
        aMap.put("glimepiride", true);
        aMap.put("glipizide", true);
        aMap.put("glyburide", true);
        aMap.put("pioglitazone", true);
        aMap.put("rosiglitazone", true);
        aMap.put("acarbose", true);
        aMap.put("insulin", true);
        aMap.put("glyburide_metformin", true);
        aMap.put("change", true);
        aMap.put("diabetesMed", true);
        aMap.put("diag_1", true);
        aMap.put("diag_2", true);
    }
    private static final Map<String, Boolean> smallNulls;
    static {
        Map<String, Boolean> aMap = new HashMap();
        initsmallNulls0(aMap);
        smallNulls = Collections.unmodifiableMap(aMap);
    }


    
   private static void initvarTypes0(Map<String, VarTypes> aMap) {
        aMap.put("admission_type_id", VarTypes.CATEGORY);
        aMap.put("diabetesMed", VarTypes.CATEGORY);
        aMap.put("chlorpropamide", VarTypes.CATEGORY);
        aMap.put("weight", VarTypes.CATEGORY);
        aMap.put("repaglinide", VarTypes.CATEGORY);
        aMap.put("payer_code", VarTypes.CATEGORY);
        aMap.put("number_diagnoses", VarTypes.NUMERIC);
        aMap.put("diag_3_desc", VarTypes.TEXT);
        aMap.put("diag_1_desc", VarTypes.TEXT);
        aMap.put("num_medications", VarTypes.NUMERIC);
        aMap.put("rosiglitazone", VarTypes.CATEGORY);
        aMap.put("glipizide", VarTypes.CATEGORY);
        aMap.put("admission_source_id", VarTypes.CATEGORY);
        aMap.put("num_lab_procedures", VarTypes.NUMERIC);
        aMap.put("glyburide", VarTypes.CATEGORY);
        aMap.put("metformin", VarTypes.CATEGORY);
        aMap.put("pioglitazone", VarTypes.CATEGORY);
        aMap.put("time_in_hospital", VarTypes.NUMERIC);
        aMap.put("A1Cresult", VarTypes.CATEGORY);
        aMap.put("glyburide_metformin", VarTypes.CATEGORY);
        aMap.put("max_glu_serum", VarTypes.CATEGORY);
        aMap.put("acarbose", VarTypes.CATEGORY);
        aMap.put("number_inpatient", VarTypes.NUMERIC);
        aMap.put("discharge_disposition_id", VarTypes.CATEGORY);
        aMap.put("change", VarTypes.CATEGORY);
        aMap.put("gender", VarTypes.CATEGORY);
        aMap.put("age", VarTypes.CATEGORY);
        aMap.put("medical_specialty", VarTypes.CATEGORY);
        aMap.put("glimepiride", VarTypes.CATEGORY);
        aMap.put("diag_2_desc", VarTypes.TEXT);
        aMap.put("race", VarTypes.CATEGORY);
        aMap.put("number_outpatient", VarTypes.NUMERIC);
        aMap.put("insulin", VarTypes.CATEGORY);
        aMap.put("number_emergency", VarTypes.NUMERIC);
        aMap.put("num_procedures", VarTypes.NUMERIC);
        aMap.put("diag_2", VarTypes.CATEGORY);
        aMap.put("diag_3", VarTypes.CATEGORY);
        aMap.put("diag_1", VarTypes.CATEGORY);
    }
    private static final Map<String, VarTypes> varTypes;
    static {
        Map<String, VarTypes> aMap = new HashMap();
        initvarTypes0(aMap);
        varTypes = Collections.unmodifiableMap(aMap);
    }


    private void combineSmallLevels(Map row) {
        Iterator i = bigLevels.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String col = (String)me.getKey();
            Object colValue = row.get(col);
            if ((colValue == null)
                || (colValue instanceof Float && Float.isNaN((Float)colValue))
                || (colValue instanceof Double && Double.isNaN((Double)colValue)))
                continue;
            Set levels = (Set)me.getValue();
            if (levels.size() == 0 || (levels.size() > 0 && !levels.contains(colValue))) {
                row.put(col, "small_count");
            }
        }
        i = smallNulls.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String col = (String)me.getKey();
            Object colValue = row.get(col);
            if((colValue == null)
                || (colValue instanceof Float && Float.isNaN((Float)colValue))
                || (colValue instanceof Double && Double.isNaN((Double)colValue))) {
                row.put(col, "small_count");
            }

        }
        i = varTypes.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String col = (String)me.getKey();
            VarTypes vtype = (VarTypes)me.getValue();
            if(vtype == VarTypes.CATEGORY || vtype == VarTypes.TEXT) {
                Object colValue = row.get(col);
                if (colValue == null || colValue.toString().isEmpty()
                    || (colValue instanceof Float && Float.isNaN((Float)colValue))
                    || (colValue instanceof Double && Double.isNaN((Double)colValue))) {
                    row.put(col, "NaN");
                }
            }
        }
    }

    private boolean deleteNAValues(Map.Entry element) {
        String v = (String)element.getValue();
        if(v.isEmpty() || NA_VALUES_LIST.contains(v)) {
            element.setValue(null);
            return true;
        }
        return false;
    }

    private float colAsNumeric(Map row, String colName) {
        Object o = row.get(colName);
        if (o instanceof Float) {
            return ((Float)o).floatValue();
        }
        if (o instanceof Double) {
            return ((Double)o).floatValue();
        }
        return stringToFloat((String)row.get(colName));
    }

    private Map processRow(Map origRow) throws ParseException {
        Map row = renameColumns(origRow);
        origRow = new HashMap(row);
        validateColumns(row.keySet());
        Iterator rowEntries = row.entrySet().iterator();
        while (rowEntries.hasNext()) {
            Map.Entry me = (Map.Entry)rowEntries.next();
            String val = (String)me.getValue();
            if (val == null)
                continue;
            if (deleteNAValues(me))
                continue;
            convertBool(me);
            parseNumericTypes(me);
        }
        addMissingIndicators(row);
        imputeMissingValues(row);
        combineSmallLevels(row);
        return row;
    }

    private void printRow(Map row) {
        Iterator i = row.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            System.out.println((String)me.getKey() + " " + me.getValue());
        }
    }

    private enum Parser {
        PERCENTAGE {
            // remove percent sign from string so it can be parsed to Double
            String parse(String x, String arg) {
                String t = x.replace("%", "").intern();
                float f = stringToFloat(t);
                return (Float.isNaN(f)) ? STRING_NA_VALUE : t;
            }
        },
        LENGTH {
            // Convert feet and inches to number of inches
            String parse(String x, String arg) {
                if(x.contains("\"") && x.contains("'")) {
                    String[] sp = x.split("'");
                    String sp1 = sp[1].replace("\"", "");
                    Double feet = stringToDouble(sp[0]);
                    Double inches = stringToDouble(sp1);
                    if (Double.isNaN(feet) || Double.isNaN(inches))
                        return STRING_NA_VALUE;
                    inches = feet * 12 + inches;
                    return String.format("%f", inches);
                } else {
                    if(x.contains("'")) {
                        Double inches = stringToDouble(x.replace("'", "").intern());
                        if (Double.isNaN(inches))
                            return STRING_NA_VALUE;
                        return String.format("%f", (inches * 12));
                    } else {
                        String inches = x.replace("\"", "").intern();
                        Double test = stringToDouble(inches);
                        if (Double.isNaN(test))
                            return STRING_NA_VALUE;
                        return inches;
                    }
                }
            }
        },
        CURRENCY {
            // Remove currency symbols from numeric cols
            String parse(String x, String arg) {
                Matcher m = CURRENCY_PATTERN.matcher(x);
                String s = m.replaceAll("").intern();
                double d = stringToDouble(s);
                return (Double.isNaN(d)) ? STRING_NA_VALUE : s;
            }
        },
        CURRENCY_NO_CENTS {
            // handle currency columns where there is no cents separator
            // FIXME @Viktor wants to reduce number of string copies
            String parse(String x, String arg) {
                validateCurrency(x);
                String s = x.replaceFirst(arg, "").intern();
                s = s.replace(" ", "").intern();
                s = s.replace(",", "").intern();
                s = s.replace(".", "").intern();
                double d = stringToDouble(s);
                return (Double.isNaN(d)) ? STRING_NA_VALUE : s;
            }
        },
        CURRENCY_CENTS_PERIOD {
            // handle currency columns where a period is the cents separator
            // FIXME @Viktor wants to reduce number of string copies
            String parse(String x, String arg) {
                validateCurrency(x);
                String s = x.replaceFirst(arg, "").intern();
                s = s.replace(" ", "").intern();
                s = s.replace(",", "").intern();
                double d = stringToDouble(s);
                return (Double.isNaN(d)) ? STRING_NA_VALUE : s;
            }
        },
        CURRENCY_CENTS_COMMA {
            // handle currency columns where a comma is the cents separator
            // FIXME @Viktor wants to reduce number of string copies
            String parse(String x, String arg) {
                validateCurrency(x);
                String s = x.replaceFirst(arg, "").intern();
                s = s.replace(" ", "").intern();
                s = s.replace(".", "").intern();
                s = s.replace(",", ".").intern();
                double d = stringToDouble(s);
                return (Double.isNaN(d)) ? STRING_NA_VALUE : s;
            }
        },
        NONSTANDARD_NA {
            // Convert non-numeric values in numeric cols to N/A
            String parse(String x, String arg) {
                double d = stringToDouble(x);
                if (Double.isInfinite(d)) {
                    return STRING_NA_VALUE;
                }
                return (Double.isNaN(d)) ? STRING_NA_VALUE : x;
            }
        },
        DATE {
            synchronized String parse(String x, String arg) {
                SimpleDateFormat parser = getDateFormatParser(arg);
                try {
                    Date d = parser.parse(x);
                    d = maybeFixDate(d, x, arg);
                    if (TIME_ONLY_PATTERN.matcher(arg).find()) {
                        UTC_EN_US_CALENDAR.set(1900, 0, 1, 0, 0, 0);
                        long base = UTC_EN_US_CALENDAR.getTimeInMillis();
                        UTC_EN_US_CALENDAR.set(1970, 0, 1, 0, 0, 0);
                        long base2 = UTC_EN_US_CALENDAR.getTimeInMillis();
                        long diff = d.getTime() - base2;
                        base = base + diff;
                        return String.format("%d", base / 1000);
                    }
                    UTC_EN_US_CALENDAR.setTime(d);
                    long m = UTC_EN_US_CALENDAR.getTimeInMillis();
                    if(arg.contains("S")) {
                        return String.format("%d", m);
                    }
                    else if(arg.contains("m")) {
                        return String.format("%d", (m + 999) / 1000);
                    }
                    return String.format("%f", (m-ORIGIN)*ONE_PER_DAY_IN_MS+1);
                } catch(Exception e) {
                    return STRING_NA_VALUE;
                }
            }
        };
        abstract String parse(String x, String arg);
    }

    private void parseNumericTypes(Map.Entry element) {
        String key = (String)element.getKey();
        if(typeConvert.containsKey(key)) {
            Parser parser = (Parser)((List)typeConvert.get(key)).get(0);
            String arg = (String)((List)typeConvert.get(key)).get(1);
            String oldValue = (String)element.getValue();
            String convertedValue = parser.parse(oldValue, arg);
            element.setValue(convertedValue);
        }
    }

    private double applySigmoid(double val) {
        return 0.5 + 0.5 * Math.tanh(0.5 * val);
    }

    private boolean matchWord(String word, String text) {
        String wordPattern = "([\\p{IsWord}\\p{No}]+)";
        Pattern r = Pattern.compile(wordPattern, Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher m = r.matcher(text);
        boolean result = false;
        while(m.find()) {
          if (m.group(1).equalsIgnoreCase(word)) {
            result = true;
            break;
          }
        }
        return result;
    }

    private static final String[] REQUIRED_COLUMNS = {"diag_1","pioglitazone","weight","glimepiride","A1Cresult","time_in_hospital","repaglinide","payer_code","acarbose","number_diagnoses","diag_3_desc","diag_1_desc","max_glu_serum","num_medications","metformin","diabetesMed","number_inpatient","rosiglitazone","discharge_disposition_id","num_lab_procedures","change","chlorpropamide","glipizide","number_emergency","age","admission_source_id","medical_specialty","diag_2_desc","glyburide_metformin","race","number_outpatient","insulin","gender","glyburide","num_procedures","diag_2","diag_3","admission_type_id"};
    private static final Set<String> REQUIRED_COLUMNS_SET = new HashSet<String>(Arrays.asList(REQUIRED_COLUMNS));
    
    private static final Map<String, List> TRANSFORMED_FEATURES = new HashMap();


    /**
     * Checks that provided currency value is string literal and can't be converted to
     * numeric directly.
     *
     * @throws IllegalArgumentException, if provided currency value is number
     */
    private static void validateCurrency(String x) {
        Matcher matcher = NUMERIC_PATTERN.matcher(x);
        if (matcher.matches()) {
            throw new IllegalArgumentException(
                String.format("Found wrong value for currency: %s", x));
        }
    }

    private boolean validateColumns(Collection columnList) {
        if(columnList.containsAll(this.REQUIRED_COLUMNS_SET)) {
            return true;
        } else {
            Set missing = new HashSet(this.REQUIRED_COLUMNS_SET);
            missing.removeAll(columnList);
            throw new IllegalArgumentException(String.format("Required columns missing: %s",
                                                             missing));
        }
    }

    public void score(Reader input, Writer output) throws IOException, ParseException {
        if (this.CSVFormatEXCEL == null) {
            throw new RuntimeException("commons CSV parser is not in classpath!");
        }
        try {
            long line = 0;
            Object csvFormat = CSVFormatEXCEL.get(null);
            Object csvParser = CSVFormatParse.invoke(csvFormat, input);
            Iterator r = (Iterator)CSVParserIterator.invoke(csvParser);
            Object header = r.next();
            int headerSize = (Integer)CSVRecordSize.invoke(header);
            output.write("Index,Prediction\n");
            while(r.hasNext()) {
                Map row = new LinkedHashMap();
                Object record = r.next();
                int recordSize = (Integer)CSVRecordSize.invoke(record);
                for(int i=0; i < recordSize; i++) {
                    row.put(CSVRecordGet.invoke(header, i), CSVRecordGet.invoke(record, i));
                }
                for (int i= recordSize; i < headerSize; i++) {
                    row.put(CSVRecordGet.invoke(header, i), "");
                }
                Map processedRow = processRow(row);
                output.write(String.format("%d,%s\n",
                            line++, Double.toString(applySigmoid(score(processedRow))).intern()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error while accessing method: " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error while invoking method of object: " + e.getMessage());
        }
    }

    public void score(String inputFilePath, String outputFilePath) throws
        IOException, FileNotFoundException, ParseException
    {
        Reader in = null;
        Writer out = null;
        try {
            in = new FileReader(inputFilePath);
            out = new FileWriter(outputFilePath);
            score(in, out);
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    
    public Iterator<Double> score(final Iterator<Map> ds) {
        return new Iterator<Double>() {

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasNext() {
                return ds.hasNext();
            }

            @Override
            public Double next() {
                try {
                    Map processedRow = processRow(ds.next());
                    return applySigmoid(score(processedRow));
                } catch (ParseException ex) {
                    throw new NoSuchElementException(ex.getMessage());
                }
            }
        };
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: input-csv output-csv");
            System.exit(1);
        }
        try {
            Prediction predictor = new Prediction();
            predictor.score(args[0], args[1]);
        } catch (Exception e) {
            System.err.println("Error while processing input file: " + e.getMessage());
            System.exit(1);
        }
    }
}

class InitBigLevels0 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
    }
}

class InitBigLevels1 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("[100-125)");
        levels.add("[50-75)");
        levels.add("[75-100)");
    }
}

class InitBigLevels2 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
    }
}

class InitBigLevels3 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("BC");
        levels.add("CM");
        levels.add("CP");
        levels.add("DM");
        levels.add("HM");
        levels.add("MC");
        levels.add("MD");
        levels.add("OG");
        levels.add("SP");
        levels.add("UN");
    }
}

class InitBigLevels4 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
    }
}

class InitBigLevels5 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
        levels.add("Up");
    }
}

class InitBigLevels6 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Down");
        levels.add("No");
        levels.add("Steady");
        levels.add("Up");
    }
}

class InitBigLevels7 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Clinic Referral");
        levels.add("Emergency Room");
        levels.add("Physician Referral");
        levels.add("Transfer from a Skilled Nursing Facility (SNF)");
        levels.add("Transfer from a hospital");
        levels.add("Transfer from another health care facility");
    }
}

class InitBigLevels8 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Down");
        levels.add("No");
        levels.add("Steady");
        levels.add("Up");
    }
}

class InitBigLevels9 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
        levels.add("Up");
    }
}

class InitBigLevels10 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
    }
}

class InitBigLevels11 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
    }
}

class InitBigLevels12 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add(">7");
        levels.add(">8");
        levels.add("Norm");
    }
}

class InitBigLevels13 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Steady");
    }
}

class InitBigLevels14 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add(">200");
        levels.add(">300");
        levels.add("Norm");
    }
}

class InitBigLevels15 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("No");
        levels.add("Yes");
    }
}

class InitBigLevels16 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Discharged to home");
        levels.add("Discharged/transferred to ICF");
        levels.add("Discharged/transferred to SNF");
        levels.add("Discharged/transferred to a long term care hospital.");
        levels.add("Discharged/transferred to another  type of inpatient care institution");
        levels.add("Discharged/transferred to another rehab fac including rehab units of a hospital.");
        levels.add("Discharged/transferred to another short term hospital");
        levels.add("Discharged/transferred to home with home health service");
        levels.add("Expired");
        levels.add("Hospice / home");
        levels.add("Left AMA");
        levels.add("Not Mapped");
    }
}

class InitBigLevels17 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Ch");
        levels.add("No");
    }
}

class InitBigLevels18 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Female");
        levels.add("Male");
    }
}

class InitBigLevels19 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("[20-30)");
        levels.add("[30-40)");
        levels.add("[40-50)");
        levels.add("[50-60)");
        levels.add("[60-70)");
        levels.add("[70-80)");
        levels.add("[80-90)");
        levels.add("[90-100)");
    }
}

class InitBigLevels20 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Cardiology");
        levels.add("Emergency/Trauma");
        levels.add("Endocrinology");
        levels.add("Family/GeneralPractice");
        levels.add("Gastroenterology");
        levels.add("InternalMedicine");
        levels.add("Nephrology");
        levels.add("ObstetricsandGynecology");
        levels.add("Oncology");
        levels.add("Orthopedics");
        levels.add("Orthopedics-Reconstructive");
        levels.add("PhysicalMedicineandRehabilitation");
        levels.add("Psychiatry");
        levels.add("Pulmonology");
        levels.add("Radiologist");
        levels.add("Surgery-Cardiovascular/Thoracic");
        levels.add("Surgery-General");
        levels.add("Surgery-Neuro");
        levels.add("Surgery-Vascular");
        levels.add("Urology");
    }
}

class InitBigLevels21 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("AfricanAmerican");
        levels.add("Asian");
        levels.add("Caucasian");
        levels.add("Hispanic");
        levels.add("Other");
    }
}

class InitBigLevels22 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Down");
        levels.add("No");
        levels.add("Steady");
        levels.add("Up");
    }
}

class InitBigLevels23 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("185");
        levels.add("197");
        levels.add("250.02");
        levels.add("250.11");
        levels.add("250.13");
        levels.add("250.22");
        levels.add("250.4");
        levels.add("250.6");
        levels.add("250.7");
        levels.add("250.8");
        levels.add("250.82");
        levels.add("276");
        levels.add("285");
        levels.add("295");
        levels.add("296");
        levels.add("38");
        levels.add("402");
        levels.add("410");
        levels.add("414");
        levels.add("415");
        levels.add("427");
        levels.add("428");
        levels.add("433");
        levels.add("434");
        levels.add("435");
        levels.add("440");
        levels.add("453");
        levels.add("458");
        levels.add("486");
        levels.add("491");
        levels.add("493");
        levels.add("507");
        levels.add("511");
        levels.add("518");
        levels.add("530");
        levels.add("535");
        levels.add("540");
        levels.add("558");
        levels.add("560");
        levels.add("562");
        levels.add("571");
        levels.add("574");
        levels.add("577");
        levels.add("578");
        levels.add("584");
        levels.add("599");
        levels.add("682");
        levels.add("715");
        levels.add("722");
        levels.add("724");
        levels.add("733");
        levels.add("780");
        levels.add("786");
        levels.add("820");
        levels.add("996");
        levels.add("997");
        levels.add("998");
        levels.add("V57");
    }
}

class InitBigLevels24 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("162");
        levels.add("197");
        levels.add("198");
        levels.add("250");
        levels.add("250.01");
        levels.add("250.02");
        levels.add("250.6");
        levels.add("250.82");
        levels.add("250.92");
        levels.add("272");
        levels.add("276");
        levels.add("278");
        levels.add("280");
        levels.add("285");
        levels.add("287");
        levels.add("295");
        levels.add("305");
        levels.add("38");
        levels.add("401");
        levels.add("402");
        levels.add("403");
        levels.add("41");
        levels.add("410");
        levels.add("411");
        levels.add("413");
        levels.add("414");
        levels.add("424");
        levels.add("425");
        levels.add("426");
        levels.add("427");
        levels.add("428");
        levels.add("440");
        levels.add("453");
        levels.add("486");
        levels.add("491");
        levels.add("492");
        levels.add("493");
        levels.add("496");
        levels.add("518");
        levels.add("530");
        levels.add("574");
        levels.add("584");
        levels.add("585");
        levels.add("599");
        levels.add("648");
        levels.add("682");
        levels.add("707");
        levels.add("780");
        levels.add("785");
        levels.add("788");
        levels.add("789");
        levels.add("8");
        levels.add("997");
        levels.add("998");
        levels.add("V42");
        levels.add("V45");
        levels.add("V58");
    }
}

class InitBigLevels25 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("197");
        levels.add("244");
        levels.add("250");
        levels.add("250.01");
        levels.add("250.02");
        levels.add("250.4");
        levels.add("250.6");
        levels.add("272");
        levels.add("276");
        levels.add("278");
        levels.add("280");
        levels.add("285");
        levels.add("287");
        levels.add("294");
        levels.add("295");
        levels.add("305");
        levels.add("357");
        levels.add("401");
        levels.add("403");
        levels.add("41");
        levels.add("414");
        levels.add("424");
        levels.add("425");
        levels.add("427");
        levels.add("428");
        levels.add("440");
        levels.add("486");
        levels.add("491");
        levels.add("493");
        levels.add("496");
        levels.add("518");
        levels.add("530");
        levels.add("571");
        levels.add("584");
        levels.add("585");
        levels.add("599");
        levels.add("682");
        levels.add("70");
        levels.add("707");
        levels.add("715");
        levels.add("780");
        levels.add("786");
        levels.add("787");
        levels.add("788");
        levels.add("998");
        levels.add("V12");
        levels.add("V15");
        levels.add("V42");
        levels.add("V43");
        levels.add("V45");
        levels.add("V58");
    }
}

class InitBigLevels26 {
    public static void init(Set levels) {
        initSubLevel0(levels);
    }
    private static void initSubLevel0(Set levels) {
        levels.add("Elective");
        levels.add("Emergency");
        levels.add("Not Available");
        levels.add("Not Mapped");
        levels.add("Urgent");
    }
}
