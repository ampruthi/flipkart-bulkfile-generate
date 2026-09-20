# Flipkart Uploader POC

Local Spring Boot 3.x + Java 21 application.

## Flow

1. Receive product fields + multipart images.
2. Send ONLY image #1 to OpenAI for product attribute extraction.
3. Upload all images to Google Drive as `<SKU>_1`, `<SKU>_2`, ...
4. Merge user fields + AI fields + Drive URLs.
5. Store one listing record in PostgreSQL with status `EXCEL_PENDING`.
6. Query listings by status.
7. Generate a copy of the Flipkart bulk Excel template and append pending listings.
8. Mark generated records as `EXCEL_GENERATED`.

## APIs

### 1. Upload product

`POST /api/v1/products/upload`

Content-Type: `multipart/form-data`

Parts:

- `product`: JSON string
- `images`: one or more image files

Example product JSON:

```json
{
  "sellerSkuId": "EAR-001",
  "mrp": 1999,
  "sellingPrice": 899,
  "fulfilment": "SELLER",
  "hsn": "7117",
  "countryOfOrigin": "India",
  "manufacturerDetails": "ABC Jewellery, Mohali, Punjab",
  "packerDetails": "ABC Jewellery, Mohali, Punjab",
  "taxCode": "GST_3",
  "brand": "ABC",
  "modelNumber": "EAR-001",
  "modelName": "Traditional Jhumki",
  "plating": "Gold",
  "attributes": {
    "Ideal For": "Women"
  }
}
```

### 2. Get listings

`GET /api/v1/listings?status=EXCEL_PENDING`

Valid statuses:

- `EXCEL_GENERATED`
- `EXCEL_PENDING`
- `OTHER`

### 3. Generate bulk Excel

`POST /api/v1/excel/generate`

The service copies the template from `src/main/resources/templates`, writes all `EXCEL_PENDING`
records into the next available rows of the `earring` sheet, saves a new file under `./output`,
and changes those records to `EXCEL_GENERATED`.

## Google Drive setup

Enable the Google Drive API and create an OAuth desktop-app credential. Put the downloaded
credentials JSON at:

`./config/google/credentials.json`

The first Drive operation starts the local OAuth browser flow and caches the token under
`./data/google-tokens`.

Optionally set `GOOGLE_DRIVE_FOLDER_ID` to upload into a specific folder.

## OpenAI setup

Set:

```text
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-5-mini
```

The model is configurable. The application sends only the first image to OpenAI.

## PostgreSQL

Start local DB:

```text
docker compose up -d
```

Then run:

```text
mvn spring-boot:run
```

## Important

The Excel template is intentionally copied unchanged into the application resources.
Generated files go to `./output` and can be deleted after Flipkart bulk upload.

The application does not call the Flipkart API yet. This version prepares the validated,
AI-enriched, Drive-backed bulk listing data for the Flipkart upload template.
