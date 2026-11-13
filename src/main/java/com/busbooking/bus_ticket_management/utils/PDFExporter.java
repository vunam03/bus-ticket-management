package com.busbooking.bus_ticket_management.utils;

import com.busbooking.bus_ticket_management.dto.TicketDetailDto;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import java.io.*;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class PDFExporter {
    private static final Color INK = new Color(23,49,44);
    private static final Color MUTED = new Color(100,119,111);
    private static final Color GREEN = new Color(23,107,82);
    private final QRCodeGenerator qr;
    public PDFExporter(QRCodeGenerator qr) { this.qr = qr; }

    public byte[] export(TicketDetailDto t, String lookupUrl) {
        try (PDDocument doc = new PDDocument();
             InputStream regular = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (regular == null) throw new IOException("Thiếu font tiếng Việt");
            var font = PDType0Font.load(doc, regular);
            var page = new PDPage(PDRectangle.A4); doc.addPage(page);
            try (var c = new PDPageContentStream(doc, page)) {
                rect(c,0,0,595,842,new Color(243,246,243));
                rect(c,0,714,595,128,INK);
                text(c,font,40,794,22,Color.WHITE,"HÀNH TRÌNH");
                text(c,font,40,771,9,new Color(185,213,197),"VÉ ĐIỆN TỬ  /  ELECTRONIC TICKET");
                String status = switch(t.status().name()) { case "CONFIRMED" -> "ĐÃ XÁC NHẬN"; case "CANCELLED" -> "ĐÃ HỦY"; default -> "CHỜ THANH TOÁN"; };
                rect(c,385,769,170,35,t.status().name().equals("CONFIRMED") ? GREEN : new Color(98,106,91));
                text(c,font,399,782,10,Color.WHITE,status);
                rect(c,30,98,535,644,Color.WHITE);
                rect(c,30,737,535,5,GREEN);
                text(c,font,50,712,9,MUTED,"CHUYẾN ĐI CỦA BẠN");
                block(c,font,t.route(),50,683,490,20,2,INK);
                block(c,font,"Tiện ích: " + t.benefits(),50,640,490,9,2,MUTED);
                field(c,font,"NHÀ XE",t.company(),50,609,300);
                boolean vip = t.seatType().name().equals("VIP");
                rect(c,405,594,140,36,vip ? new Color(250,237,199) : new Color(232,242,235));
                text(c,font,422,608,11,vip ? new Color(124,86,15) : GREEN,vip ? "HẠNG VIP" : "TIÊU CHUẨN");
                line(c,50,575,545,575,false);
                text(c,font,50,549,9,MUTED,"GIỜ KHỞI HÀNH");
                text(c,font,50,514,29,GREEN,t.departure().format(DateTimeFormatter.ofPattern("HH:mm")));
                text(c,font,50,493,11,INK,t.departure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                text(c,font,280,549,9,MUTED,"SỐ GHẾ");
                block(c,font,t.seat(),280,514,225,29,1,GREEN);
                text(c,font,280,493,10,MUTED,"Một hành khách / một vé");
                line(c,50,473,545,473,false);
                field(c,font,"HÀNH KHÁCH",t.passenger(),50,450,480);
                field(c,font,"PHƯƠNG THỨC",switch(t.method().name()){case "CASH" -> "Tiền mặt";case "BANK_TRANSFER" -> "Chuyển khoản";default -> "Ví điện tử";},50,391,215);
                field(c,font,"TRẠNG THÁI THANH TOÁN",switch(t.paymentStatus().name()){case "PAID" -> "Đã thanh toán";case "REFUNDED" -> "Đã hoàn tiền";default -> "Chưa thanh toán";},300,391,230);
                rect(c,50,300,495,48,new Color(236,245,239));
                text(c,font,65,319,11,INK,"TỔNG GIÁ VÉ");
                String amount = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(t.total()) + " đ";
                block(c,font,amount,300,316,230,21,1,GREEN);
                line(c,30,281,565,281,true);
                var img = PDImageXObject.createFromByteArray(doc,qr.png(lookupUrl),"Ticket QR");
                c.drawImage(img,50,139,125,125);
                text(c,font,200,251,10,GREEN,"MÃ TRA CỨU VÉ");
                block(c,font,t.code(),200,230,340,10,2,INK);
                block(c,font,"Giữ vé điện tử để tra cứu hành trình. QR yêu cầu đăng nhập tài khoản đã đặt vé.",200,193,325,9,3,MUTED);
                text(c,font,50,120,8,MUTED,"Vui lòng kiểm tra ngày giờ, số ghế và xác nhận điểm đón với nhà xe.");
                text(c,font,40,74,9,INK,"HÀNH TRÌNH  /  Chúc bạn có một chuyến đi tốt đẹp.");
                text(c,font,40,55,8,MUTED,"Thanh toán thử nghiệm. Không phải hóa đơn thuế.");
                text(c,font,516,55,8,MUTED,"01 / 01");
            }
            doc.save(out);return out.toByteArray();
        } catch(IOException e) { throw new IllegalStateException("Không xuất được PDF.",e); }
    }
    private static void rect(PDPageContentStream c,float x,float y,float w,float h,Color color)throws IOException{c.setNonStrokingColor(color);c.addRect(x,y,w,h);c.fill();}
    private static void text(PDPageContentStream c,PDType0Font f,float x,float y,float size,Color color,String value)throws IOException{
        c.beginText();c.setFont(f,size);c.setNonStrokingColor(color);c.newLineAtOffset(x,y);c.showText(value == null ? "—" : value.replaceAll("[\\p{Cntrl}]"," "));c.endText();
    }
    private static void line(PDPageContentStream c,float x,float y,float x2,float y2,boolean dashed)throws IOException{c.setStrokingColor(new Color(216,226,219));c.setLineWidth(1);c.setLineDashPattern(dashed?new float[]{4,4}:new float[]{},0);c.moveTo(x,y);c.lineTo(x2,y2);c.stroke();}
    private static void field(PDPageContentStream c,PDType0Font f,String label,String value,float x,float y,float width)throws IOException{text(c,f,x,y,8,MUTED,label);block(c,f,value,x,y-22,width,12,2,INK);}
    private static void block(PDPageContentStream c,PDType0Font f,String value,float x,float y,float width,float size,int maxLines,Color color)throws IOException{
        String s = value == null ? "—" : value.replaceAll("[\\p{Cntrl}]"," ").trim();
        for(int row=0;row<maxLines && !s.isEmpty();row++){
            int end=s.length();while(end>1 && f.getStringWidth(s.substring(0,end))/1000*size>width)end--;
            if(end<s.length() && row<maxLines-1){int space=s.lastIndexOf(' ',end);if(space>0)end=space;}
            String part=s.substring(0,end).trim();
            if(row==maxLines-1 && end<s.length()){while(!part.isEmpty() && f.getStringWidth(part+"…")/1000*size>width)part=part.substring(0,part.length()-1);part+="…";}
            text(c,f,x,y-row*(size+5),size,color,part);s=s.substring(end).trim();
        }
    }
}
