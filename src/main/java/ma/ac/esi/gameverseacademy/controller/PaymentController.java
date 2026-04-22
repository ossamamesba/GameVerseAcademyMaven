package ma.ac.esi.gameverseacademy.controller;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import ma.ac.esi.gameverseacademy.model.*;
import ma.ac.esi.gameverseacademy.repository.PaymentRepository;
import ma.ac.esi.gameverseacademy.util.SecurityUtils;
import java.io.IOException;
import java.util.List;
public class PaymentController extends HttpServlet {
    private PaymentRepository payRepo = new PaymentRepository();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!SecurityUtils.checkAccess(request, response, "ADMIN")) return;
        List<Payment> payments = payRepo.getAllPayments();
        request.setAttribute("payments", payments);
        request.getRequestDispatcher("/WEB-INF/views/payments.jsp").forward(request, response);
    }
}
