# Windows OCR Desktop Application Plan

## Goal
Create a Windows desktop application that can extract all text and numbers from user-supplied images (screenshots, photos, scanned documents) and present the recognized text in a convenient format for copying or saving.

## Key Features
- **Image Ingestion**: Allow users to load images via file picker, drag-and-drop, or system clipboard.
- **Optical Character Recognition (OCR)**: Run OCR on the selected image to detect English and optional additional languages.
- **Result Display**: Show recognized text, highlight the regions in the image, and provide copy-to-clipboard functionality.
- **Batch Processing**: Support multiple images in a session and optional bulk export to text files.
- **History & Export**: Maintain a session history and enable export to TXT/RTF.

## Technology Stack
- **UI Framework**: Windows Presentation Foundation (WPF) on .NET 6/7 for modern Windows desktop development.
- **OCR Engine**: [Tesseract OCR](https://github.com/tesseract-ocr/tesseract) via the `Tesseract` .NET wrapper (`Tesseract 5.x` NuGet package). This choice avoids UWP restrictions of `Windows.Media.Ocr` and works for pure desktop apps.
- **Image Processing**: `ImageSharp` or `System.Drawing.Common` for image pre-processing (grayscale, thresholding, resizing) to improve OCR accuracy.
- **Dependency Management**: NuGet for third-party libraries.

## Architecture Overview
```
┌───────────────────────────┐
│        WPF Views          │
│  (MainWindow, History)    │
└────────────┬──────────────┘
             │Data Binding (MVVM)
┌────────────▼──────────────┐
│         ViewModels        │
│ (MainViewModel, HistoryVM)│
└────────────┬──────────────┘
             │Commands/Events
┌────────────▼──────────────┐
│        Services Layer     │
│  IOService, OcrService,   │
│  ImageProcessingService   │
└────────────┬──────────────┘
             │Tesseract APIs
┌────────────▼──────────────┐
│     OCR Engine (DLL)      │
└───────────────────────────┘
```

- **MVVM Pattern**: Keeps UI responsive and testable. Commands trigger OCR asynchronously.
- **OcrService**: Wraps the Tesseract engine initialization, language data loading, image preprocessing, and text extraction.
- **ImageProcessingService**: Applies optional filters (binarization, contrast) to boost OCR success rate.

## Implementation Steps
1. **Project Setup**
   - Create a new WPF project targeting .NET 6.
   - Install NuGet packages: `Tesseract`, `CommunityToolkit.Mvvm`, `SixLabors.ImageSharp`.
   - Add the `tessdata` directory with required language data files (e.g., `eng.traineddata`).

2. **UI Layout**
   - `MainWindow.xaml`: Contains image preview area, recognized text pane, toolbar buttons (Open, Paste, Capture, Export), and status bar.
   - Use `Grid`/`DockPanel` for layout, with `ListView` to display session history.

3. **ViewModel Logic**
   - `MainViewModel` exposes properties such as `SelectedImage`, `RecognizedText`, and `IsProcessing`.
   - Commands: `OpenImageCommand`, `PasteImageCommand`, `RunOcrCommand`, `ExportCommand`.
   - Use `AsyncRelayCommand` from CommunityToolkit for asynchronous OCR operations to keep UI responsive.

4. **Screen Snipping Capture**
   - Implement a `CaptureSnippetCommand` that launches an overlay window allowing the user to drag-select a rectangular region.
   - Use a transparent, full-screen WPF window with `Topmost` and `WindowStyle=None` to display the overlay and capture mouse events.
   - Upon selection completion, leverage `System.Drawing` or `Windows.Graphics.Capture` to copy the pixels within the region into a `Bitmap`.
   - Automatically route the captured `Bitmap` through the existing OCR pipeline and populate `SelectedImage`/`RecognizedText`.
   - Persist the captured image to the session history for later review.

5. **OCR Service**
   - Initialize Tesseract engine once with path to `tessdata` and selected languages.
   - Provide methods like `Task<OcrResult> ExtractTextAsync(Stream imageStream, string language)`.
   - Convert `Pix` results into structured data (text blocks, bounding boxes).

6. **Image Preprocessing**
   - Normalize orientation (auto-rotate based on EXIF).
   - Convert to grayscale, apply adaptive thresholding, and optionally deskew.
   - Allow users to toggle preprocessing options from settings.

7. **Result Presentation**
   - Display recognized text in a scrollable `TextBox` with copy/save buttons.
   - Overlay bounding boxes on the preview image to show detection accuracy.
   - Provide `Export to TXT` and `Copy All` features.

8. **Additional Enhancements**
   - **Screenshot Capture**: Integrate with Windows snipping APIs to capture the full screen or a user-defined region without leaving the app.
   - **Hotkey Support**: Global hotkey to trigger OCR on clipboard image.
   - **Language Selection**: UI dropdown to choose additional OCR languages.
   - **Error Handling**: Graceful fallback when OCR fails or languages missing.
   - **Logging**: Add structured logging (e.g., `Serilog`) for diagnostics.

## Sample OCR Service Snippet
```csharp
public class OcrService : IOcrService, IDisposable
{
    private readonly TesseractEngine _engine;

    public OcrService(string tessDataPath, string languages = "eng")
    {
        _engine = new TesseractEngine(tessDataPath, languages, EngineMode.Default);
    }

    public async Task<string> ExtractTextAsync(Stream imageStream)
    {
        return await Task.Run(() =>
        {
            using var pixImage = Pix.LoadFromMemory(imageStream.ReadAllBytes());
            using var page = _engine.Process(pixImage);
            return page.GetText();
        });
    }

    public void Dispose()
    {
        _engine.Dispose();
    }
}
```
*Use an extension method `ReadAllBytes()` to read the stream; ensure `ImageSharp` handles conversions when needed.*

## Sample Screen Snip Overlay Snippet
```csharp
public async Task<BitmapSource?> CaptureSnippetAsync()
{
    var overlay = new SnipOverlayWindow();
    if (overlay.ShowDialog() != true)
    {
        return null;
    }

    var region = overlay.SelectedRegion;
    using var bitmap = new Bitmap((int)region.Width, (int)region.Height);
    using var graphics = Graphics.FromImage(bitmap);
    graphics.CopyFromScreen((int)region.X, (int)region.Y, 0, 0, bitmap.Size, CopyPixelOperation.SourceCopy);

    return await Task.Run(() => bitmap.ToBitmapSource());
}
```
*`SnipOverlayWindow` is a borderless WPF window capturing mouse drag to define `SelectedRegion`; extension `ToBitmapSource()` converts to WPF-friendly image.*

## Testing Strategy
- **Unit Tests**: Mock `OcrService` to verify ViewModel commands and state transitions.
- **Integration Tests**: Run OCR on sample images with known expected text.
- **Manual QA**: Check UI interactions, clipboard operations, and multi-language recognition.

## Deployment
- Package using `dotnet publish -c Release -r win10-x64 --self-contained true` for a standalone executable.
- Include `tessdata` folder alongside the binaries.
- Optionally create an installer with `WiX` or `MSIX` if distribution requires automatic updates.

## Future Enhancements
- Table recognition with layout preservation.
- Handwriting support via Microsoft Cognitive Services OCR API fallback.
- Translation pipeline to convert recognized text into other languages.
- Cloud sync of history and preferences.
