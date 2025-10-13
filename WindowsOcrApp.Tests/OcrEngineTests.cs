using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using WindowsOcrApp;

namespace WindowsOcrApp.Tests;

[TestClass]
public class OcrEngineTests
{
    [TestMethod]
    public void TryGetDefaultTessdataPath_ReturnsNull_WhenProgramFilesMissing()
    {
        var originalValue = Environment.GetEnvironmentVariable("PROGRAMFILES");
        try
        {
            Environment.SetEnvironmentVariable("PROGRAMFILES", null);

            var engine = new OcrEngine();
            var tessdataPath = engine.TryGetDefaultTessdataPath();

            Assert.IsNull(tessdataPath);
        }
        finally
        {
            Environment.SetEnvironmentVariable("PROGRAMFILES", originalValue);
        }
    }
}
