package ru.edu.pr01;

/** Исходная проверка преподавателя сохранена; ниже добавлены собственные граничные проверки. */
public class SelfCheck {
    private static int failed = 0;

    public static void main(String[] args) {
        boolean ok = false;
        try { ok = (OrderCalculator.calculateBase(2, 100.0) == 200.0 && Math.abs(OrderCalculator.calculateTotal(2,100.0,10.0,20.0)-216.0)<0.001); } catch (Exception e) { System.out.println("FAIL exception " + e.getMessage()); }
        System.out.println(ok ? "PASS" : "FAIL  complete TODO methods");
        if (!ok) failed++;

        System.out.println("--- Собственные проверки ---");
        // 1. корректное типовое значение (вариант 1: НДС 10 %): 12 × 2490,50 = 29 886; −5 % = 28 391,70; +10 % = 31 230,87
        check("типовое значение, НДС 10 %", Math.abs(OrderCalculator.calculateTotal(12, 2490.50, 5) - 31230.87) < 0.001);
        // 2. нижние границы: количество 1 включено, 0 исключено; скидка 0 включена, −0,01 исключена
        check("количество 1 допустимо", OrderCalculator.isValid(1, 100, 0));
        check("количество 0 отклонено", !OrderCalculator.isValid(0, 100, 0));
        check("цена 0,01 допустима", OrderCalculator.isValid(1, 0.01, 0));
        check("цена 0 отклонена", !OrderCalculator.isValid(1, 0, 0));
        check("скидка −0,01 отклонена", !OrderCalculator.isValid(1, 100, -0.01));
        // 3. верхние границы: 10 000 и 5 000 000 и 30 % включены, следующее значение исключено
        check("количество 10 000 допустимо", OrderCalculator.isValid(10_000, 100, 0));
        check("количество 10 001 отклонено", !OrderCalculator.isValid(10_001, 100, 0));
        check("цена 5 000 000 допустима", OrderCalculator.isValid(1, 5_000_000, 0));
        check("цена 5 000 000,01 отклонена", !OrderCalculator.isValid(1, 5_000_000.01, 0));
        check("скидка 30 % допустима", OrderCalculator.isValid(1, 100, 30));
        check("скидка 30,01 % отклонена", !OrderCalculator.isValid(1, 100, 30.01));
        // 4. пустое или отсутствующее значение -> контролируемая ошибка, а не исключение
        check("пустая строка", OrderCalculator.processLine("").startsWith("Ошибка"));
        check("пустая цена", OrderCalculator.processLine("5;;10").startsWith("Ошибка"));
        // 5. неверный формат
        check("цена «abc»", OrderCalculator.processLine("5;abc;10").startsWith("Ошибка"));
        check("дробное количество", OrderCalculator.processLine("2.5;100;10").startsWith("Ошибка"));
        check("лишнее поле", OrderCalculator.processLine("1;2;3;4").startsWith("Ошибка"));
        // 6. повтор данных: калькулятор не хранит состояния, одинаковый ввод даёт одинаковый результат
        check("повтор строки даёт тот же результат", OrderCalculator.processLine("3;999.99;10").equals(OrderCalculator.processLine("3;999.99;10")));
        // 7. скидка не трактуется как сумма в рублях: 100 руб. со скидкой 10 = 90, а не 0
        check("скидка в процентах", Math.abs(OrderCalculator.applyDiscount(100, 10) - 90) < 1e-9);
        // 8. округление до копеек: 3 × 0,1 = 0,30000000000000004 -> 0,30
        check("округление до копеек", OrderCalculator.roundMoney(3 * 0.1) == 0.30);

        System.out.println(failed == 0 ? "ИТОГ: все проверки пройдены" : "ИТОГ: не пройдено проверок: " + failed);
    }

    private static void check(String name, boolean condition) {
        System.out.println((condition ? "PASS  " : "FAIL  ") + name);
        if (!condition) failed++;
    }
}
