# multi-modal-ditherer
Dither images using custom palettes and patterns!

## `https://api.jcooksey.dev/ditherer/dither` Endpoint

Accepts a multipart form submission containing an input image and a palette image, applies dithering, and returns the processed result as a Base64-encoded PNG in JSON.

### Request

- **Method:** `POST`
- **Content-Type:** `multipart/form-data`

#### Required form fields
- `inputImage`: source image file
- `palette`: palette image file

#### Optional form fields
- `pattern`: dithering pattern to apply [`hilbert` or `linear`]
  - Defaults to `hilbert` when omitted

### Processing Notes

- The client submits standard `multipart/form-data`.
- The form must include exactly two image fields: `inputImage` and `palette`.
- Requests with missing, extra, or malformed image fields are rejected.
- In the AWS API Gateway → Lambda flow, the multipart request body is received by Lambda as Base64-encoded data and decoded server-side before form parsing.

### Response

#### Success (`200 OK`)
```json
{
  "resultImage": "<base64-encoded PNG>"
}
```

#### Error (`400 Bad Request`)
```json
{
  "error": "[validation or parsing error message]"
}
```
