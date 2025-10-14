using Microsoft.Win32;
using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media.Imaging;

namespace WindowsOcrApp;

public partial class MainWindow : Window
{
    private readonly OcrEngine _ocrEngine = new();
    private BitmapImage? _currentImage;

    public MainWindow()
    {
        InitializeComponent();
        TessdataPathTextBox.Text = _ocrEngine.TryGetDefaultTessdataPath();
    }

    private void OpenImageButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new System.Windows.Forms.OpenFileDialog
        {
            Title = "Open Image",
            Filter = "Image Files|*.png;*.jpg;*.jpeg;*.bmp;*.tif;*.tiff"
        };

        if (dialog.ShowDialog() == true)
        {
            LoadImage(new Uri(dialog.FileName));
        }
    }

    private async void RunOcrButton_Click(object sender, RoutedEventArgs e)
    {
        if (_currentImage is null)
        {
            System.Windows.MessageBox.Show("Please load or capture an image first.", "No Image", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        if (string.IsNullOrWhiteSpace(TessdataPathTextBox.Text))
        {
            System.Windows.MessageBox.Show("Please provide a tessdata directory path.", "Missing tessdata", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        ToggleUi(false);
        OcrResultTextBox.Text = "Running OCR...";

        try
        {
            using var stream = new MemoryStream();
            EncodeBitmap(_currentImage, stream);
            stream.Position = 0;
            var text = await Task.Run(() => _ocrEngine.Recognize(stream, TessdataPathTextBox.Text!));
            OcrResultTextBox.Text = text;
        }
        catch (Exception ex)
        {
            OcrResultTextBox.Text = string.Empty;
            System.Windows.MessageBox.Show($"Failed to run OCR: {ex.Message}", "OCR Error", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            ToggleUi(true);
        }
    }

    private void SnipButton_Click(object sender, RoutedEventArgs e)
    {
        var bitmap = ScreenSnipper.CaptureRegion();
        if (bitmap is null)
        {
            return;
        }

        var image = new BitmapImage();
        using var memory = new MemoryStream();
        bitmap.Save(memory, System.Drawing.Imaging.ImageFormat.Png);
        memory.Position = 0;
        image.BeginInit();
        image.CacheOption = BitmapCacheOption.OnLoad;
        image.StreamSource = memory;
        image.EndInit();
        image.Freeze();
        _currentImage = image;
        PreviewImage.Source = _currentImage;
    }

    private void BrowseTessdataButton_Click(object sender, RoutedEventArgs e)
    {
        using var dialog = new System.Windows.Forms.FolderBrowserDialog
        {
            Description = "Select the tessdata directory"
        };

        if (dialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
        {
            TessdataPathTextBox.Text = dialog.SelectedPath;
        }
    }

    private void LoadImage(Uri uri)
    {
        var image = new BitmapImage();
        image.BeginInit();
        image.CacheOption = BitmapCacheOption.OnLoad;
        image.UriSource = uri;
        image.EndInit();
        image.Freeze();
        _currentImage = image;
        PreviewImage.Source = _currentImage;
        OcrResultTextBox.Clear();
    }

    private static void EncodeBitmap(BitmapSource source, Stream stream)
    {
        var encoder = new PngBitmapEncoder();
        encoder.Frames.Add(BitmapFrame.Create(source));
        encoder.Save(stream);
    }

    private void ToggleUi(bool isEnabled)
    {
        OpenImageButton.IsEnabled = isEnabled;
        SnipButton.IsEnabled = isEnabled;
        RunOcrButton.IsEnabled = isEnabled;
        BrowseTessdataButton.IsEnabled = isEnabled;
    }
}
