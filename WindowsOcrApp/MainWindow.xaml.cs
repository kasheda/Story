using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;

namespace WindowsOcrApp;

public partial class MainWindow : Window
{
    private readonly OcrEngine _ocrEngine = new();
    private string? _tessdataPath;

    public MainWindow()
    {
        InitializeComponent();
        _tessdataPath = ResolveTessdataPath();
        if (_tessdataPath is null)
        {
            OcrResultTextBox.Text = "Install Tesseract OCR or place a 'tessdata' folder next to the app executable.";
        }
    }

    private async void SnipButton_Click(object sender, RoutedEventArgs e)
    {
        var tessdata = _tessdataPath ?? ResolveTessdataPath();
        if (tessdata is null)
        {
            MessageBox.Show(
                "Tesseract tessdata folder not found. Install Tesseract OCR or place a tessdata folder next to the app.",
                "Missing tessdata",
                MessageBoxButton.OK,
                MessageBoxImage.Warning);
            return;
        }

        try
        {
            SnipButton.IsEnabled = false;
            OcrResultTextBox.Text = "Select the area you want to capture...";

            var bitmap = ScreenSnipper.CaptureRegion();
            if (bitmap is null)
            {
                OcrResultTextBox.Text = "Capture cancelled.";
                return;
            }

            OcrResultTextBox.Text = "Running OCR...";

            using var memory = new MemoryStream();
            bitmap.Save(memory, System.Drawing.Imaging.ImageFormat.Png);
            memory.Position = 0;

            var text = await Task.Run(() => _ocrEngine.Recognize(memory, tessdata));
            OcrResultTextBox.Text = string.IsNullOrWhiteSpace(text)
                ? "No text detected in the captured image."
                : text.Trim();
        }
        catch (Exception ex)
        {
            OcrResultTextBox.Text = string.Empty;
            MessageBox.Show($"Failed to capture or process the image: {ex.Message}", "OCR Error", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            SnipButton.IsEnabled = true;
            _tessdataPath = tessdata;
        }
    }

    private string? ResolveTessdataPath()
    {
        var defaultPath = _ocrEngine.TryGetDefaultTessdataPath();
        if (!string.IsNullOrWhiteSpace(defaultPath))
        {
            return defaultPath;
        }

        var localPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tessdata");
        return Directory.Exists(localPath) ? localPath : null;
    }
}
