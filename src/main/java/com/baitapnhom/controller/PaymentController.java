package com.baitapnhom.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baitapnhom.config.MailBuilder;
import com.baitapnhom.config.PaypalPaymentIntent;
import com.baitapnhom.config.PaypalPaymentMethod;
import com.baitapnhom.entity.HoaDon;
import com.baitapnhom.entity.Mail;
import com.baitapnhom.entity.PaymentMethod;
import com.baitapnhom.entity.SanPham;
import com.baitapnhom.entity.User;
import com.baitapnhom.renpository.HoaDonRepository;
import com.baitapnhom.renpository.SanPhamRepository;
import com.baitapnhom.renpository.UserRepository;
import com.baitapnhom.service.EmailService;
import com.baitapnhom.service.EmailServices;
import com.baitapnhom.service.HoaDonService;
import com.baitapnhom.service.PaypalService;
import com.baitapnhom.utils.Utils;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

@Controller
public class PaymentController extends Thread {
    public static final String URL_PAYPAL_SUCCESS = "card/success";
    public static final String URL_PAYPAL_CANCEL = "card/cancel";
    private Logger log = LoggerFactory.getLogger(getClass());
    private Long Id;
    @Autowired
    private  EmailServices emailServices;
    Thread t;
    @Autowired
    private PaypalService paypalService;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private HoaDonService hoaDonService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private HttpSession session;
    

    @GetMapping("/phuongthucthanhtoan")
    public String phuongthucthanhtoan(Model model,HttpSession  session) {
        model.addAttribute("payment", new PaymentMethod());
        Double priceDouble = 168.000;
        model.addAttribute("price", priceDouble);
        
        System.out.println("fdffdfd"+session.getAttribute("userimage").toString());
        model.addAttribute("userimage", session.getAttribute("userimage").toString());
       
        return "/card/card_phuongthucthanhtoan";
    }
    
    @PostMapping("/card_phuongthucthanhtoan")
    public String card_phuongthucthanhtoan(Model model, HttpSession session, @CookieValue(value = "isNameCookie", defaultValue = "defaultCookieValue") String cookieValue) {
        model.addAttribute("isNameCookie", cookieValue);
        model.addAttribute("isNameSession", session.getAttribute("isNameSession"));
        model.addAttribute("userimage", session.getAttribute("userimage").toString());
        model.addAttribute("payment", new PaymentMethod());
        System.out.println(session.getAttribute("tong"));
        return "/card/card_phuongthucthanhtoan";
    }
    
    @GetMapping(URL_PAYPAL_CANCEL)
    public String cancelPay(){
        HoaDon hoaDon=hoaDonRepository.findById(Id).orElseThrow();
        hoaDonRepository.delete(hoaDon);
        return "/card/cancel";
    }
    
    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId){
        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if(payment.getState().equals("approved")){
                HoaDon hoaDon = hoaDonService.findById(Id).orElseThrow();
                hoaDon.setWasPay(true);
                return "card/success";
            }
        } catch (PayPalRESTException e) {
            log.error(e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/pay")
    public String pay(Model model,@ModelAttribute PaymentMethod paymentMethod, HttpSession session, @CookieValue(value = "isNameCookie", defaultValue = "defaultCookieValue") String cookieValue,HttpServletRequest request, @RequestParam(name = "price") double price){
        System.out.println("12345s4ad654s65d = " + paymentMethod.getPaymentMethodEnum().getDisplayValue());
        int i=0;
        List<SanPham> listnew=new ArrayList<>();
     // System.out.println("12345s4ad654s65d = " + paymentMethod.getPaymentMethodEnum().getDisplayValue());
        LocalDate date = LocalDate.now();
        Calendar calendar = Calendar.getInstance();
        String timeString = calendar.getTime().toString();
        String[] words=timeString.split("\\s");
        String Username;
      if (!cookieValue.equals("defaultCookieValue"))
          Username=cookieValue;
      else
          Username=session.getAttribute("isNameSession").toString();
      User user=userRepository.findByUsername(Username).orElseThrow();
     
        String dateTime = date.toString() + " " + words[3];
        
        String pttt = paymentMethod.getPaymentMethodEnum().getDisplayValue();
        
        if(paymentMethod.getPaymentMethodEnum().getDisplayValue() == "Paypal") {
            String cancelUrl = Utils.getBaseURL(request) + "/" + URL_PAYPAL_CANCEL;
            String successUrl = Utils.getBaseURL(request) + "/" + URL_PAYPAL_SUCCESS;
            
//            System.out.println(cancelUrl + "---000---" + successUrl);
         
            List<SanPham> litsp=(List<SanPham>)session.getAttribute("card");
            for(SanPham sanpham : litsp) {
                HoaDon hoaDon = new HoaDon(dateTime, price, pttt, false);
                SanPham sanphamnew=sanPhamRepository.findById(sanpham.getId()).orElseThrow();
                sanphamnew.setSoluong(sanphamnew.getSoluong()-sanpham.getSoluong());
                hoaDon.setSanpham_id(sanphamnew);
              
                hoaDon.setSoluong(sanpham.getSoluong());
                hoaDon.setGia(sanpham.getGia());
                hoaDon.setStatus(false);
                //gán user
                hoaDon.setUser(user);
                hoaDonRepository.save(hoaDon);
                sanPhamRepository.save(sanphamnew);
                
            }
            session.setAttribute("soluong",null);
            session.setAttribute("card", listnew);
            this.start();
            session.setAttribute("tong", 0);
            
            
      
            
            try {
                Payment payment = paypalService.createPayment(
                        price,
                        "USD",
                        PaypalPaymentMethod.paypal,
                        PaypalPaymentIntent.sale,
                        "payment description",
                        cancelUrl,
                        successUrl);
                for(Links links : payment.getLinks()){
                    if(links.getRel().equals("approval_url")){
                        return "redirect:" + links.getHref();
                    }
                }
            } catch (PayPalRESTException e) {
                log.error(e.getMessage());
            }
            return "redirect:/";
        }
        else {
           // this.start();
            List<SanPham> litsp=(List<SanPham>)session.getAttribute("card");
           
      
            for(SanPham sanpham : litsp) {
                HoaDon hoaDon = new HoaDon(dateTime, price, pttt, true);
                SanPham sanphamnew=sanPhamRepository.findById(sanpham.getId()).orElseThrow();
                hoaDon.setSanpham_id(sanphamnew);
                sanphamnew.setSoluong(sanphamnew.getSoluong()-sanpham.getSoluong());
                System.out.println(sanphamnew.getSoluong()+"   "+sanpham.getSoluong());
                hoaDon.setSoluong(sanpham.getSoluong());
                hoaDon.setGia(sanpham.getGia());
                hoaDon.setStatus(false);
                //gán user
                hoaDon.setUser(user);
                sanPhamRepository.save(sanphamnew);
                hoaDonRepository.save(hoaDon);
            }
            
            
            session.setAttribute("soluong", null);
            session.setAttribute("card", listnew);
            session.setAttribute("tong", 0);
            model.addAttribute("isNameCookie", cookieValue);
            model.addAttribute("isNameSession", session.getAttribute("isNameSession"));
            model.addAttribute("userimage", session.getAttribute("userimage").toString());
            return "/card/success";
        }
    }
    
    public void start() {

        t = new Thread(this);
        t.start();

    }
    
    @Override
    public void run() {

   System.out.println("Luoongf ddang chay");
        final Mail mail = new MailBuilder()
                .From("letamxi87@gmail.com") // For gmail, this field is ignored.
                .To("thanhb1910139@student.ctu.edu.vn")
                .Template("email/mail-hoadon.html")
                .AddContext("subject", "Test Email")
                .AddContext("content","noi dung")
                .Subject("Hello")
                .createMail();
        try {
            emailServices.sendHTMLEmail(mail);
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        t.stop();
        t = null;

    }
    
}
