# Windows OCR Desktop Application

This WPF application provides a graphical interface to extract text from images or ad-hoc screen snippets using the open-source Tesseract OCR engine.

## Features
- Load local image files in common formats (PNG, JPEG, BMP, TIFF).
- Capture a custom screen region via a snipping overlay and run OCR on the captured bitmap.
- Configure the location of the `tessdata` folder used by Tesseract.
- Display the recognized text with copy-friendly formatting.

## Getting Started
1. Install the [Tesseract OCR Windows installer](https://github.com/UB-Mannheim/tesseract/wiki).
2. Ensure the `tessdata` directory from the installation is available (default: `C:\Program Files\Tesseract-OCR\tessdata`).
3. From the repository root (where `WindowsOcrApp.sln` lives), restore packages, build, and run the tests:
   ```bash
   dotnet restore WindowsOcrApp.sln
   dotnet build WindowsOcrApp.sln
   dotnet test WindowsOcrApp.sln
   ```
4. Launch the application from the repository root:
   ```bash
   dotnet run --project WindowsOcrApp/WindowsOcrApp.csproj
   ```

When running for the first time, verify that the tessdata path shown in the header matches your installation. Use the **Browse** button if you installed Tesseract elsewhere.

## Publishing a Windows executable
If you would like a distributable `.exe` without relying on `dotnet run`, publish the project from a Windows machine with the .NET SDK installed:

```bash
dotnet publish -c Release -r win-x64 --self-contained false
```

This produces `WindowsOcrApp.exe` inside `bin/Release/net6.0-windows/win-x64/publish/`. Copy that folder to another Windows PC with the appropriate .NET Desktop Runtime and Tesseract tessdata assets. Use the `--self-contained true` option to bundle the runtime at the cost of a larger download.

Because the container environment used for development does not include the .NET SDK or Windows build tools, an executable cannot be generated directly here. Running the publish command above on your Windows machine will create the runnable file.

## Notes
- The snipping overlay uses Windows Forms interop to provide a lightweight capture UI.
- Additional languages can be enabled by installing the corresponding `.traineddata` files into the tessdata directory and modifying the language parameter inside `OcrEngine` if required.
