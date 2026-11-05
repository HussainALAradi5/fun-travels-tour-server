-- Flag images for every stored country that has none (flagcdn.com, HTTPS). The country list itself comes from
-- the country directory API on startup (CountryDirectoryImporter), not from this script.

UPDATE countries
SET flag_png_url = 'https://flagcdn.com/w80/' || lower(country_code) || '.png'
WHERE country_code ~ '^[A-Za-z]{2}$' AND (flag_png_url IS NULL OR flag_png_url IN ('', 'null'));

UPDATE countries
SET flag_svg_url = 'https://flagcdn.com/' || lower(country_code) || '.svg'
WHERE country_code ~ '^[A-Za-z]{2}$' AND (flag_svg_url IS NULL OR flag_svg_url IN ('', 'null'));
