# ADR-0001: Modular Monolith + Layered Architecture

## Quyết định
Dùng một Spring Boot backend duy nhất, chia module theo domain và tổ chức mỗi module theo Controller/Service/Repository.

## Lý do
- Phù hợp project cá nhân.
- Dễ học, test, deploy và giải thích khi phỏng vấn.
- Tránh độ phức tạp không cần thiết của microservices.

## Hệ quả
Nếu hệ thống lớn lên, module boundaries đã rõ nên có thể tách service sau này.
