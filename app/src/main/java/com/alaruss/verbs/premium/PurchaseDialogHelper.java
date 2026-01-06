package com.alaruss.verbs.premium;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.alaruss.verbs.R;

public class PurchaseDialogHelper {

    public interface PurchaseDialogCallback {
        void onBuyClicked();
        void onCancelled();
    }

    public static void showPurchaseDialog(Activity activity, BillingManager billingManager,
                                           String customMessage, PurchaseDialogCallback callback) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_purchase, null);

        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        if (customMessage != null) {
            messageView.setText(customMessage);
        }

        TextView priceView = dialogView.findViewById(R.id.price_text);
        if (billingManager != null) {
            priceView.setText(billingManager.getFormattedPrice());
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.premium_title)
                .setView(dialogView)
                .setPositiveButton(R.string.buy_button, (dialog, which) -> {
                    if (callback != null) {
                        callback.onBuyClicked();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (callback != null) {
                        callback.onCancelled();
                    }
                })
                .show();
    }

    public static void showFavoritesLimitDialog(Activity activity, BillingManager billingManager,
                                                  int limit, PurchaseDialogCallback callback) {
        String message = activity.getString(R.string.favorites_limit_message, limit);
        showPurchaseDialog(activity, billingManager, message, callback);
    }

    public static void showThemePurchaseDialog(Activity activity, BillingManager billingManager,
                                                 PurchaseDialogCallback callback) {
        String message = activity.getString(R.string.theme_requires_premium);
        showPurchaseDialog(activity, billingManager, message, callback);
    }
}
