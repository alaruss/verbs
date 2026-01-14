package com.alaruss.verbs.premium;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.alaruss.verbs.R;

public class PurchaseDialogHelper {

    public static void showPurchaseDialog(Activity activity, BillingManager billingManager,
                                          String customMessage, int favoritesLimit, PurchaseDialogCallback callback) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_purchase, null);

        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        if (customMessage != null) {
            messageView.setText(customMessage);
        }

        TextView favoritesView = dialogView.findViewById(R.id.benefit_favorites);
        favoritesView.setText(activity.getString(R.string.premium_benefit_favorites, favoritesLimit));

        TextView priceView = dialogView.findViewById(R.id.price_text);
        if (billingManager != null) {
            String price = billingManager.getFormattedPrice();
            if (price != null) {
                priceView.setText(price);
            } else {
                // Hide price if unavailable - will be shown when billing is ready
                priceView.setVisibility(View.GONE);
            }
        } else {
            priceView.setVisibility(View.GONE);
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
        showPurchaseDialog(activity, billingManager, message, limit, callback);
    }

    public interface PurchaseDialogCallback {
        void onBuyClicked();

        void onCancelled();
    }
}
