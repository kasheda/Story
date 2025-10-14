using System;
using System.Drawing;
using System.Windows.Forms;

namespace WindowsOcrApp;

public static class ScreenSnipper
{
    public static Bitmap? CaptureRegion()
    {
        using var overlay = new SnippingForm();
        return overlay.ShowDialog() == DialogResult.OK ? overlay.CapturedBitmap : null;
    }

    private sealed class SnippingForm : Form
    {
        private Rectangle _selection;
        private Point _startPoint;
        private bool _isDragging;

        public Bitmap? CapturedBitmap { get; private set; }

        public SnippingForm()
        {
            FormBorderStyle = FormBorderStyle.None;
            StartPosition = FormStartPosition.Manual;
            Bounds = Screen.PrimaryScreen?.Bounds ?? throw new InvalidOperationException("No screen detected");
            DoubleBuffered = true;
            TopMost = true;
            Cursor = Cursors.Cross;
            BackColor = Color.White;
            Opacity = 0.25;
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            base.OnMouseDown(e);
            _isDragging = true;
            _startPoint = e.Location;
            _selection = new Rectangle(e.Location, Size.Empty);
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            if (!_isDragging)
            {
                return;
            }

            var x1 = Math.Min(e.X, _startPoint.X);
            var y1 = Math.Min(e.Y, _startPoint.Y);
            var x2 = Math.Max(e.X, _startPoint.X);
            var y2 = Math.Max(e.Y, _startPoint.Y);
            _selection = new Rectangle(x1, y1, x2 - x1, y2 - y1);
            Invalidate();
        }

        protected override void OnMouseUp(MouseEventArgs e)
        {
            base.OnMouseUp(e);
            _isDragging = false;

            if (_selection.Width <= 0 || _selection.Height <= 0)
            {
                DialogResult = DialogResult.Cancel;
                return;
            }

            CapturedBitmap = new Bitmap(_selection.Width, _selection.Height);
            using (var graphics = Graphics.FromImage(CapturedBitmap))
            {
                graphics.CopyFromScreen(_selection.Location, Point.Empty, _selection.Size);
            }

            DialogResult = DialogResult.OK;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            if (_selection.Width <= 0 || _selection.Height <= 0)
            {
                return;
            }

            using var pen = new Pen(Color.Red, 2);
            e.Graphics.DrawRectangle(pen, _selection);
        }

        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (keyData == Keys.Escape)
            {
                DialogResult = DialogResult.Cancel;
                return true;
            }

            return base.ProcessCmdKey(ref msg, keyData);
        }
    }
}
