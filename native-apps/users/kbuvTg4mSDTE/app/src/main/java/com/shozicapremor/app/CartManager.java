package com.shozicapremor.app;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    public static class CartItem {
        public String shoeName;
        public double price;
        public int quantity;
        public String size;
        public String imageUrl;

        public CartItem(String shoeName, double price, int quantity, String size, String imageUrl) {
            this.shoeName = shoeName;
            this.price = price;
            this.quantity = quantity;
            this.size = size;
            this.imageUrl = imageUrl;
        }
    }

    private static CartManager instance;
    private final List<CartItem> items = new ArrayList<>();

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.shoeName.equals(item.shoeName) && existing.size.equals(item.size)) {
                existing.quantity += item.quantity;
                return;
            }
        }
        items.add(item);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    // MANDATORY LOCAL CALCULATION RULES:
    public double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : items) {
            subtotal += item.price * item.quantity;
        }
        return subtotal;
    }

    public double getTax(double subtotal) {
        return subtotal * 0.08; // 8% sales tax
    }

    public double getDiscount(double subtotal) {
        if (subtotal > 200.0) {
            return subtotal * 0.10; // VIP 10% discount on orders over $200
        }
        return 0.0;
    }

    public double getGrandTotal() {
        double subtotal = getSubtotal();
        return subtotal + getTax(subtotal) - getDiscount(subtotal);
    }
}