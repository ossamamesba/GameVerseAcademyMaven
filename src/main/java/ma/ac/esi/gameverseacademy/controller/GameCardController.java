package ma.ac.esi.gameverseacademy.controller;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import ma.ac.esi.gameverseacademy.model.*;
import ma.ac.esi.gameverseacademy.repository.*;
import ma.ac.esi.gameverseacademy.util.PdfGenerator;
import ma.ac.esi.gameverseacademy.util.SecurityUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
public class GameCardController extends HttpServlet {
    private GameCardRepository cardRepo = new GameCardRepository();
    private ModRepository modRepo = new ModRepository();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!SecurityUtils.checkAccess(request, response, null)) return;
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");
        if ("buy".equals(action)) {
            int modId = Integer.parseInt(request.getParameter("modId"));
            Mod mod = modRepo.getModById(modId);
            request.setAttribute("mod", mod);
            request.getRequestDispatcher("/WEB-INF/views/buyMod.jsp").forward(request, response);
        } else if ("delete".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            cardRepo.deleteCard(id);
            response.sendRedirect(request.getContextPath() + "/cards");
        } else if ("pdf".equals(action)) {
            int cardId = Integer.parseInt(request.getParameter("cardId"));
            GameCard card = cardRepo.getCardById(cardId);
            PaymentRepository payRepo = new PaymentRepository();
            List<Payment> payments = payRepo.getAllPayments();
            Payment payment = payments.stream().filter(p -> p.getGameCardId() == cardId).findFirst().orElse(null);
            if (card != null && payment != null) {
                byte[] pdf = PdfGenerator.generateReceipt(card, payment);
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=receipt_" + cardId + ".pdf");
                response.getOutputStream().write(pdf);
            } else {
                response.sendRedirect(request.getContextPath() + "/cards");
            }
        } else {
            List<GameCard> cards = "ADMIN".equals(user.getRole()) ? cardRepo.getAllCards() : cardRepo.getCardsByUser(user.getLogin());
            request.setAttribute("cards", cards);
            request.getRequestDispatcher("/WEB-INF/views/cards.jsp").forward(request, response);
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!SecurityUtils.checkAccess(request, response, null)) return;
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        int modId = Integer.parseInt(request.getParameter("modId"));
        String cardHolder = request.getParameter("cardHolder");
        String cardNumber = request.getParameter("cardNumber").replaceAll("\\s", "");
        String cardLast4 = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : "0000";
        double price = Double.parseDouble(request.getParameter("price"));
        GameCard gc = new GameCard();
        gc.setModId(modId);
        gc.setUserLogin(user.getLogin());
        gc.setCardHolder(cardHolder);
        gc.setCardNumberLast4(cardLast4);
        gc.setPrice(price);
        int cardId = cardRepo.addCard(gc);
        Payment payment = new Payment();
        payment.setGameCardId(cardId);
        payment.setAmount(price);
        payment.setStatus("SUCCESS");
        payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        PaymentRepository payRepo = new PaymentRepository();
        payRepo.addPayment(payment);
        modRepo.incrementDownloads(modId);
        response.sendRedirect(request.getContextPath() + "/cards?action=pdf&cardId=" + cardId);
    }
}
