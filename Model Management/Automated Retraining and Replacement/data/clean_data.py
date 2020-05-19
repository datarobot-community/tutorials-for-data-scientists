import pandas as pd

df = pd.read_csv('time_series_covid19_confirmed_global.csv')

# filter to Australia only and clean
df = df[df['Country/Region'] == 'Australia'] \
        .melt(id_vars=['Province/State', 'Country/Region', 'Lat', 'Long']) \
        .assign(date = lambda x: pd.to_datetime(x['variable'], format='%m/%d/%y')) \
        .drop(columns='variable') \
        .rename(columns={'value': 'cases'})

df.to_csv('australian_cases.csv', index=False)
