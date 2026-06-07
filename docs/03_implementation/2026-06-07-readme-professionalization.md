# Chuyên Nghiệp Hóa README Cho 4 Dự Án

Ngày: 2026-06-07  
Người thực hiện: Thanh Quan  

## Tổng quan
Để chuẩn bị tốt nhất cho quá trình đi ứng tuyển công việc, toàn bộ 4 file `README.md` của các dự án trong workspace đã được viết lại. Chúng tích hợp trực tiếp các chỉ số kỹ thuật thực tế đạt được, bảng giải pháp nghiệp vụ và sơ đồ trực quan Mermaid để thu hút nhà tuyển dụng.

## Chi tiết các tài liệu đã cập nhật

1. **Inventory & Flash Sale Engine** ([README.md](file:///D:/projects/inventory-flashsale-system/README.md)):
   - **Nội dung**: Giới thiệu về an toàn concurrency (Redis Lock, Optimistic Lock), outbox pattern kết hợp Kafka và đối soát đồng bộ đa kênh (TikTok, Shopee).
   - **Trực quan**: Sequence diagram mô tả chi tiết luồng đặt chỗ an toàn.

2. **Hospital Management System (HMS)** ([README.md](file:///D:/projects/hospital-management-system/README.md)):
   - **Nội dung**: Phân tích sâu kiến trúc DDD Monolith (5 Maven modules), quy trình phân quyền RBAC và giải pháp bảo mật PHI (CCCD mã hóa AES-GCM và hash SHA-256).
   - **Trực quan**: Quy trình hành trình khám chữa bệnh khép kín của bệnh nhân.

3. **AI-Powered Hospital Knowledge Assistant** ([README.md](file:///D:/projects/chatbot-hospital-system/README.md)):
   - **Nội dung**: Giới thiệu hệ thống AI Agent hỗ trợ RAG phân quyền, cơ chế lọc quyền trước truy vấn (Permission-first RAG), chánh ảo giác nguồn trích dẫn (Citation Validation) và cơ chế xử lý tài liệu bất đồng bộ (Redis/RQ Worker).
   - **Trực quan**: Sơ đồ màng lọc an toàn thông tin PHI trước khi gửi prompt tới LLM.

4. **Flash-Sale Concurrency Engine** ([README.md](file:///d:/projects/tipjs-project/xxxx.com-section-ddd-24-27042025/Flash-Sale-Concurrency-Engine/README.md)):
   - **Nội dung**: Bản mô tả Concurrency Lab, so sánh 4 chiến lược trừ kho (Unsafe, Conditional DB, Redis Lua, Redis Lua + Compensation).
   - **Trực quan**: So sánh luồng xử lý khóa DB với luồng trừ kho trên cache có bù trừ. Nêu bật chỉ số đo đạc JMeter thực tế (throughput tăng **9.1 lần**, latency nhanh hơn **11.4 lần**).

## Ý nghĩa đối với ứng tuyển
- Tránh việc phóng đại số liệu, đưa ra các con số thực tế được chứng thực bởi bộ test case/benchmark.
- Các sơ đồ Mermaid giúp nhà tuyển dụng nắm bắt nhanh cấu trúc hệ thống và luồng dữ liệu chỉ trong vài giây đọc lướt.
