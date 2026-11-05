-- Reference countries with dial codes, so sign-up has a country-code list on a new database,
-- and flag images for every country that has none (flagcdn.com, HTTPS).

INSERT INTO countries (code, famous_name, official_name, country_code, dial_code, mobile_number_length, created_at, updated_at)
VALUES
    ('COU-REF-BH', 'Bahrain', 'Kingdom of Bahrain', 'BH', '+973', 8, now(), now()),
    ('COU-REF-SA', 'Saudi Arabia', 'Kingdom of Saudi Arabia', 'SA', '+966', 9, now(), now()),
    ('COU-REF-AE', 'United Arab Emirates', 'United Arab Emirates', 'AE', '+971', 9, now(), now()),
    ('COU-REF-KW', 'Kuwait', 'State of Kuwait', 'KW', '+965', 8, now(), now()),
    ('COU-REF-QA', 'Qatar', 'State of Qatar', 'QA', '+974', 8, now(), now()),
    ('COU-REF-OM', 'Oman', 'Sultanate of Oman', 'OM', '+968', 8, now(), now()),
    ('COU-REF-JO', 'Jordan', 'Hashemite Kingdom of Jordan', 'JO', '+962', 9, now(), now()),
    ('COU-REF-EG', 'Egypt', 'Arab Republic of Egypt', 'EG', '+20', 10, now(), now()),
    ('COU-REF-LB', 'Lebanon', 'Lebanese Republic', 'LB', '+961', 8, now(), now()),
    ('COU-REF-IQ', 'Iraq', 'Republic of Iraq', 'IQ', '+964', 10, now(), now()),
    ('COU-REF-TR', 'Turkey', 'Republic of Türkiye', 'TR', '+90', 10, now(), now()),
    ('COU-REF-IN', 'India', 'Republic of India', 'IN', '+91', 10, now(), now()),
    ('COU-REF-PK', 'Pakistan', 'Islamic Republic of Pakistan', 'PK', '+92', 10, now(), now()),
    ('COU-REF-PH', 'Philippines', 'Republic of the Philippines', 'PH', '+63', 10, now(), now()),
    ('COU-REF-GB', 'United Kingdom', 'United Kingdom of Great Britain and Northern Ireland', 'GB', '+44', 10, now(), now()),
    ('COU-REF-US', 'United States', 'United States of America', 'US', '+1', 10, now(), now())
ON CONFLICT (country_code) DO NOTHING;

-- Existing countries keep their own values; only missing dial codes and flags are filled in.
UPDATE countries c
SET dial_code = COALESCE(NULLIF(c.dial_code, ''), r.dial_code),
    mobile_number_length = COALESCE(c.mobile_number_length, r.len)
FROM (VALUES ('BH', '+973', 8), ('SA', '+966', 9), ('AE', '+971', 9), ('KW', '+965', 8), ('QA', '+974', 8),
             ('OM', '+968', 8), ('JO', '+962', 9), ('EG', '+20', 10), ('LB', '+961', 8), ('IQ', '+964', 10),
             ('TR', '+90', 10), ('IN', '+91', 10), ('PK', '+92', 10), ('PH', '+63', 10), ('GB', '+44', 10),
             ('US', '+1', 10)) AS r(country_code, dial_code, len)
WHERE c.country_code = r.country_code;

UPDATE countries
SET flag_png_url = 'https://flagcdn.com/w80/' || lower(country_code) || '.png'
WHERE country_code ~ '^[A-Za-z]{2}$' AND (flag_png_url IS NULL OR flag_png_url IN ('', 'null'));

UPDATE countries
SET flag_svg_url = 'https://flagcdn.com/' || lower(country_code) || '.svg'
WHERE country_code ~ '^[A-Za-z]{2}$' AND (flag_svg_url IS NULL OR flag_svg_url IN ('', 'null'));
