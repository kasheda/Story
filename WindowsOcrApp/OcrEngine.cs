using System;
using System.IO;
using Tesseract;

namespace WindowsOcrApp;

public class OcrEngine
{
    public string Recognize(Stream imageStream, string tessdataPath)
    {
        if (!Directory.Exists(tessdataPath))
        {
            throw new DirectoryNotFoundException($"tessdata directory not found: {tessdataPath}");
        }

        using var engine = new TesseractEngine(tessdataPath, "eng", EngineMode.Default);
        using var pix = Pix.LoadFromMemory(ReadFully(imageStream));
        using var page = engine.Process(pix);
        return page.GetText();
    }

    public string? TryGetDefaultTessdataPath()
    {
        var programFiles = Environment.GetEnvironmentVariable("PROGRAMFILES");
        if (string.IsNullOrWhiteSpace(programFiles))
        {
            return null;
        }

        var tessdata = Path.Combine(programFiles, "Tesseract-OCR", "tessdata");
        return Directory.Exists(tessdata) ? tessdata : null;
    }

    private static byte[] ReadFully(Stream stream)
    {
        if (stream is MemoryStream memoryStream)
        {
            return memoryStream.ToArray();
        }

        using var memory = new MemoryStream();
        stream.CopyTo(memory);
        return memory.ToArray();
    }
}
