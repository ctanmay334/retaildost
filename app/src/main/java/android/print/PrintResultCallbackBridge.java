package android.print;

import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;

/**
 * PrintResultCallbackBridge
 * ─────────────────────────
 * A package-scoped bridge to instantiate abstract print callback classes
 * whose constructors are package-private in the Android SDK.
 */
public class PrintResultCallbackBridge {

    public static LayoutResultCallback createLayoutCallback(
            final LayoutResultCallbackDelegate delegate
    ) {
        return new LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                delegate.onLayoutFinished(info, changed);
            }

            @Override
            public void onLayoutFailed(CharSequence error) {
                delegate.onLayoutFailed(error);
            }

            @Override
            public void onLayoutCancelled() {
                delegate.onLayoutCancelled();
            }
        };
    }

    public static WriteResultCallback createWriteCallback(
            final WriteResultCallbackDelegate delegate
    ) {
        return new WriteResultCallback() {
            @Override
            public void onWriteFinished(PageRange[] pages) {
                delegate.onWriteFinished(pages);
            }

            @Override
            public void onWriteFailed(CharSequence error) {
                delegate.onWriteFailed(error);
            }

            @Override
            public void onWriteCancelled() {
                delegate.onWriteCancelled();
            }
        };
    }

    public interface LayoutResultCallbackDelegate {
        void onLayoutFinished(PrintDocumentInfo info, boolean changed);
        void onLayoutFailed(CharSequence error);
        void onLayoutCancelled();
    }

    public interface WriteResultCallbackDelegate {
        void onWriteFinished(PageRange[] pages);
        void onWriteFailed(CharSequence error);
        void onWriteCancelled();
    }
}
