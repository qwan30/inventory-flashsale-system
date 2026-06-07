# Triển khai Thiết lập CI/CD cho 3 Dự án

Ngày: 2026-06-07  
Người thực hiện: Thanh Quan  

## Tổng quan
Để giải quyết các khoảng trống về tự động hóa kiểm thử và triển khai ghi nhận trong các dự án Hospital & Inventory Systems, chúng tôi đã xây dựng cấu hình CI/CD hoàn chỉnh bằng GitHub Actions và Docker Compose cho 3 dự án.

## Chi tiết các thiết lập đã thực hiện

### 1. Dự án Omnichannel Inventory & Flash Sale System
- **CI Workflow ([ci.yml](file:///D:/projects/inventory-flashsale-system/.github/workflows/ci.yml))**:
  - Loại bỏ cờ `-DskipTests` để tự động hóa chạy test tích hợp backend (chạy qua Maven reactor verify).
  - Tích hợp dịch vụ Docker giúp **Testcontainers** khởi động thành công các dịch vụ local MySQL, Redis và Kafka.
  - Tự động hóa build Docker images cho API và Admin UI và push lên GHCR khi test hoàn tất thành công.
- **CD Workflow ([cd.yml](file:///D:/projects/inventory-flashsale-system/.github/workflows/cd.yml))**:
  - Triển khai kéo container và chạy qua SSH Docker Compose khi CI trên branch `main` thành công.

### 2. Dự án Hospital Management System (HMS)
- **CI Workflow ([ci.yml](file:///D:/projects/hospital-management-system/.github/workflows/ci.yml))**:
  - Tích hợp thêm bước chạy unit test frontend có coverage (`npm run test:unit:coverage`).
  - Cấu hình tự động đóng gói Docker images đẩy lên GHCR.
- **CD Workflow ([cd.yml](file:///D:/projects/hospital-management-system/.github/workflows/cd.yml))**:
  - Triển khai toàn bộ container (Postgres, Backend, Frontend) kết hợp với bộ công cụ Observability (Loki, Grafana, Tempo, Prometheus) lên VPS.

### 3. Dự án AI-Powered Hospital Knowledge Assistant
- **Contract Verification**:
  - Viết script kiểm tra tương thích API Contract ([verify_contracts.py](file:///d:/projects/chatbot-hospital-system/app/backend/scripts/verify_contracts.py)) giúp tự động phát hiện sớm lệch route/endpoint giữa Frontend Next.js và Backend FastAPI.
- **CI/CD Workflows**:
  - Tích hợp bước chạy verify contract vào CI workflow ([test-backend.yaml](file:///D:/projects/chatbot-hospital-system/.github/workflows/test-backend.yaml)).
  - Khởi tạo mới workflow [cd.yml](file:///d:/projects/chatbot-hospital-system/.github/workflows/cd.yml) triển khai qua SSH Docker Compose.

## Ý nghĩa đối với công việc tương lai
- Quy trình CD sử dụng trigger `workflow_run` chỉ chạy khi CI thành công giúp bảo vệ môi trường live khỏi mã nguồn lỗi.
- Mọi thay đổi về endpoint API ở Backend giờ đây sẽ được kiểm chứng hợp đồng tự động ở CI, đảm bảo Frontend không bị lỗi 404 khi gọi dữ liệu.
