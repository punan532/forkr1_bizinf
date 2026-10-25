package ru.edu.pr01;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Расчёт стоимости заказа: базовая сумма, скидка, НДС и итог.
 * Вариант 1 (зачётная книжка ...56): ставка НДС = 10 %.
 * Все пороги собраны в константах этого класса, чтобы правило менялось в одном месте.
 */
public class OrderCalculator {

    /** Вариант 1: ставка НДС, %. Единственное место, где задаётся ставка. */
    public static final double VAT_RATE = 10.0;

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 10_000;
    public static final double MAX_UNIT_PRICE = 5_000_000.0;
    public static final double MIN_DISCOUNT = 0.0;
    public static final double MAX_DISCOUNT = 30.0;

    private static final Locale RU = Locale.forLanguageTag("ru-RU");

    // ---------------------------------------------------------------- проверки

    /** A1: количество 1..10 000, цена (0; 5 000 000], скидка 0..30 %. */
    public static boolean isValid(int quantity, double unitPrice, double discountPercent) {
        return validationError(quantity, unitPrice, discountPercent) == null;
    }

    /**
     * Возвращает понятное сообщение об ошибке или null, если данные корректны.
     * Один метод отвечает и за isValid, и за текст ошибки, поэтому границы не расходятся.
     */
    public static String validationError(int quantity, double unitPrice, double discountPercent) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            return "Количество должно быть от " + MIN_QUANTITY + " до " + MAX_QUANTITY + ", получено " + quantity;
        }
        if (!Double.isFinite(unitPrice) || unitPrice <= 0 || unitPrice > MAX_UNIT_PRICE) {
            return "Цена единицы должна быть больше 0 и не больше 5 000 000 руб., получено " + unitPrice;
        }
        if (!Double.isFinite(discountPercent) || discountPercent < MIN_DISCOUNT || discountPercent > MAX_DISCOUNT) {
            return "Скидка должна быть от 0 до 30 %, получено " + discountPercent;
        }
        return null;
    }

    private static void requirePercent(double percent, String what) {
        if (!Double.isFinite(percent) || percent < 0 || percent > 100) {
            throw new IllegalArgumentException(what + " должна быть в диапазоне 0..100 %, получено " + percent);
        }
    }

    // ---------------------------------------------------------------- расчёт

    /** Базовая сумма = количество × цена единицы. */
    public static double calculateBase(int quantity, double unitPrice) {
        return quantity * unitPrice;
    }

    /** Сумма после скидки = база × (1 − скидка / 100). Скидка задаётся в процентах, а не в рублях. */
    public static double applyDiscount(double base, double discountPercent) {
        requirePercent(discountPercent, "Скидка");
        return base * (1.0 - discountPercent / 100.0);
    }

    /** НДС считается от суммы ПОСЛЕ скидки. */
    public static double calculateVat(double discounted, double vatPercent) {
        requirePercent(vatPercent, "Ставка НДС");
        return discounted * vatPercent / 100.0;
    }

    /** A2: база → скидка → НДС от суммы со скидкой → итог, округлённый до копеек. */
    public static double calculateTotal(int quantity, double unitPrice, double discountPercent, double vatPercent) {
        String error = validationError(quantity, unitPrice, discountPercent);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        double base = calculateBase(quantity, unitPrice);
        double discounted = applyDiscount(base, discountPercent);
        double vat = calculateVat(discounted, vatPercent);
        return roundMoney(discounted + vat);
    }

    /** Итог по ставке НДС варианта. */
    public static double calculateTotal(int quantity, double unitPrice, double discountPercent) {
        return calculateTotal(quantity, unitPrice, discountPercent, VAT_RATE);
    }

    /** Округление до копеек по правилу HALF_UP через BigDecimal (у double нет точного представления 0,1). */
    public static double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String formatRub(double value) {
        return String.format(RU, "%.2f руб.", roundMoney(value));
    }

    // ---------------------------------------------------------------- ввод и вывод

    /** Разбор одной строки «количество;цена;скидка». Возвращает текст отчёта или текст ошибки. */
    public static String processLine(String line) {
        if (line == null || line.isBlank()) {
            return "Ошибка: пустая строка";
        }
        String[] parts = line.split(";", -1);
        if (parts.length != 3) {
            return "Ошибка: ожидается 3 значения через «;», получено " + parts.length;
        }
        int quantity;
        double price;
        double discount;
        try {
            quantity = parseQuantity(parts[0]);
            price = parseDecimal(parts[1], "цена");
            discount = parseDecimal(parts[2], "скидка");
        } catch (IllegalArgumentException e) {
            return "Ошибка: " + e.getMessage();
        }
        String error = validationError(quantity, price, discount);
        if (error != null) {
            return "Ошибка: " + error;
        }
        return report(quantity, price, discount, VAT_RATE);
    }

    static int parseQuantity(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("количество не указано");
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("количество должно быть целым числом, получено «" + s + "»");
        }
    }

    static double parseDecimal(String raw, String name) {
        String s = raw == null ? "" : raw.trim().replace(',', '.');
        if (s.isEmpty()) {
            throw new IllegalArgumentException(name + " не указана");
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " должна быть числом, получено «" + raw.trim() + "»");
        }
    }

    static String report(int quantity, double price, double discount, double vatPercent) {
        double base = calculateBase(quantity, price);
        double discounted = applyDiscount(base, discount);
        double vat = calculateVat(discounted, vatPercent);
        return String.format(RU, "Базовая сумма: %s; скидка %.0f %%: %s; НДС %.0f %%: %s; итого: %s",
                formatRub(base), discount, formatRub(base - discounted),
                vatPercent, formatRub(vat), formatRub(roundMoney(discounted + vat)));
    }

    /** Без аргументов — ввод с клавиатуры; с аргументом — обработка CSV-файла (первая строка — заголовок). */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            List<String> lines = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                System.out.println("Строка " + (i + 1) + " [" + lines.get(i) + "] -> " + processLine(lines.get(i)));
            }
            return;
        }
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.print("Количество: ");
        String q = in.hasNextLine() ? in.nextLine() : "";
        System.out.print("Цена единицы, руб.: ");
        String p = in.hasNextLine() ? in.nextLine() : "";
        System.out.print("Скидка, %: ");
        String d = in.hasNextLine() ? in.nextLine() : "";
        System.out.println(processLine(q + ";" + p + ";" + d));
    }
}
