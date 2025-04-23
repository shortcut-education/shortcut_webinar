package ru.shortcut.app;

import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import ru.shortcut.client.CbrRestClient;
import ru.shortcut.client.NotificationRestClient;
import ru.shortcut.repository.FeeRepository;
import ru.shortcut.repository.PaymentRepository;
import ru.shortcut.repository.UserRepository;

import java.util.Currency;

public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRestClient notificationRestClient;

    @Autowired
    private CbrRestClient cbrRestClient;

    @Transactional
    public void processPayment(double amount, Currency currency, long recipientId) {
        double amountInRub = amount * cbrRestClient.dorequest().getRates().get(currency.getCode());
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId);
        Payment payment = new Payment(amountInRub, user, recipientId);
        paymentRepository.save(payment);

        if (amountInRub < 1000) {
            Fee fee = new Fee(amountInRub * 0.015, user);
            feeRepository.save(fee);
        }

        if (amountInRub > 1000) {
            Fee fee = new Fee(amountInRub * 0.01, user);
            feeRepository.save(fee);
        }

        if (amountInRub > 5000) {
            Fee fee = new Fee(amountInRub * 0.005, user);
            feeRepository.save(fee);
        }

        try {
            notificationRestClient.notify(payment);
        } catch (Exception e) {
            // do nothing
        }
    }
}
