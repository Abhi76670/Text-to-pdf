package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.File;

/** Lives in android.print so it can reach the package-private print callbacks. */
public class PdfPrint {
    public interface Done { void run(boolean ok); }

    public static void print(final PrintDocumentAdapter adapter, final File out, final Done done) {
        PrintAttributes attrs = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 72, 72))
                .setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0)) // CSS @page supplies the 20 mm A4 margins
                .build();
        adapter.onLayout(null, attrs, new CancellationSignal(), new PrintDocumentAdapter.LayoutResultCallback() {
            @Override public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                try {
                    final ParcelFileDescriptor fd = ParcelFileDescriptor.open(out,
                            ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
                    adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, fd, new CancellationSignal(), new PrintDocumentAdapter.WriteResultCallback() {
                        @Override public void onWriteFinished(PageRange[] pages) { close(fd); done.run(true); }
                        @Override public void onWriteFailed(CharSequence error) { close(fd); done.run(false); }
                    });
                } catch (Exception e) { done.run(false); }
            }
            @Override public void onLayoutFailed(CharSequence error) { done.run(false); }
        }, null);
    }

    private static void close(ParcelFileDescriptor fd) { try { fd.close(); } catch (Exception ignored) { } }
}
