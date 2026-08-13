# Kế hoạch commit đề xuất

Không push toàn bộ project bằng một commit `final`.

```text
chore: initialize CineVerse project structure
docs: add architecture and business rules
feat: add authentication and user profile
feat: add movie and cinema catalog
feat: add showtime scheduling with conflict validation
feat: add seat map and concurrency-safe seat holding
feat: add booking and pricing flow
feat: add mock payment and ticket issuance
feat: add customer booking pages
feat: add admin management pages
test: add core business logic tests
chore: add Docker and CI workflow
docs: add setup guide and seat locking ADR
```

Nếu đã có code hoàn chỉnh trước khi tạo repo, có thể chia commit theo nhóm file tương ứng để lịch sử Git vẫn dễ đọc, nhưng không nên giả mạo ngày tháng hay giả vờ đã phát triển theo thứ tự khác thực tế.
