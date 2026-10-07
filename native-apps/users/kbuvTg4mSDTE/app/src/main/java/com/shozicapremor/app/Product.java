package com.shozicapremor.app;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private final String id;
    private final String name;
    private final String category;
    private final double price;
    private final String description;
    private final String[] imageUrls;

    public Product(String id, String name, String category, double price, String description, String[] imageUrls) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.imageUrls = imageUrls;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public String[] getImageUrls() { return imageUrls; }

    private static List<Product> catalog;

    public static synchronized List<Product> getCatalog() {
        if (catalog == null) {
            catalog = new ArrayList<>();
            // Shoe 1: Air Zoom Max (Running)
            catalog.add(new Product("1", "Air Zoom Max Extreme", "Running", 185.00,
                    "Elevate your stride. Boasts dual responsive nitrogen-infused Zoom capsules coupled with customized carbon-fiber flight plate geometry.",
                    new String[]{
                            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=500&q=80"
                    }));

            // Shoe 2: Retro Sneaker Premium (Casual)
            catalog.add(new Product("2", "Retro Sneaker Gold-Elite", "Casual", 130.00,
                    "Vibrant custom retro aesthetics merged with premium durable leather trims and an Ortholite high-density comfort sockliner for all-day style.",
                    new String[]{
                            "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=500&q=80"
                    }));

            // Shoe 3: Royal Leather Oxford (Formal)
            catalog.add(new Product("3", "Royal Oxford Classique", "Formal", 240.00,
                    "Handcrafted luxury. Features full grain premium Italian leather uppers, precise Goodyear welted leather soles, and rich golden inner lining.",
                    new String[]{
                            "https://images.unsplash.com/photo-1533867617858-e7b97e060509?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1614252369475-531eba835eb1?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1481841587433-6226118991c3?auto=format&fit=crop&w=500&q=80"
                    }));

            // Shoe 4: Elite Trail Runner (Running)
            catalog.add(new Product("4", "Elite Mountain Trail Runner", "Running", 195.00,
                    "Conquer rough mountain paths with engineered water-repellant ripstop fabric and deep lugged sticky Vibram outsoles designed for maximum traction.",
                    new String[]{
                            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80",
                            "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=500&q=80"
                    }));
        }
        return catalog;
    }

    public static Product getProductById(String id) {
        List<Product> items = getCatalog();
        for (int i = 0; i < items.size(); i++) {
            Product p = items.get(i);
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }
}