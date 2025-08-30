# VNEconomy (VND) — No Vault

Plugin kinh tế riêng không sử dụng Vault, cung cấp API để các plugin tùy biến hook vào.
- Đơn vị tiền tệ: Việt Nam (₫), định dạng ngắn 1.2M ₫ (có thể tắt).
- Lưu dữ liệu: YAML / SQLite / MySQL (HikariCP).
- PlaceholderAPI: `%vneco_balance%`, `%vneco_balance_formatted%`, `%vneco_top_1_name%`, `%vneco_top_1_amount%` (đến top_10).
- Top player, thông báo + âm thanh tuỳ chỉnh.
- Lệnh: `/vnmoney [player]`, `/vnmoney top`, `/vnmoney pay <player> <amount>`, `/vneco give|set|take <player> <amount>`, `/vneco reload`.

## Build
```bash
mvn -q -e -DskipTests package
```
Tệp phát hành ở `target/VNEconomy-1.0.0-shaded.jar`.

## API sử dụng trong plugin khác
```java
import org.fox.vneconomy.api.EconomyAPI;

double bal = EconomyAPI.get().getBalance(playerUUID);
EconomyAPI.get().give(playerUUID, 100000); // +100,000 ₫
EconomyAPI.get().take(playerUUID, 50000);  // -50,000 ₫
EconomyAPI.get().set(playerUUID, 1234567); // =1,234,567 ₫
boolean ok = EconomyAPI.get().has(playerUUID, 10000); // checkMoney
```

## Placeholder
- `%vneco_balance%` — số dư thô
- `%vneco_balance_formatted%` — số dư định dạng VND
- `%vneco_top_1_name%`, `%vneco_top_1_amount%` … đến 10

## Quyền
- `vneco.use` — dùng /money
- `vneco.pay` — dùng /money pay
- `vneco.admin` — dùng /eco

## 1. Plugin của bạn sẽ hook API như sau

Trong plugin khác, chỉ cần import class EconomyAPI từ VNEconomy.
Ví dụ:

```yaml
import org.fox.vneconomy.api.EconomyAPI;
import java.util.UUID;

public class MyPlugin {
public void testEco(UUID uuid) {
// Lấy số dư
double bal = EconomyAPI.get().getBalance(uuid);
System.out.println("Số dư: " + bal);

        // Cộng tiền
        EconomyAPI.get().give(uuid, 50000);

        // Trừ tiền
        EconomyAPI.get().take(uuid, 20000);

        // Set thẳng số dư
        EconomyAPI.get().set(uuid, 1000000);

        // Kiểm tra có đủ tiền không
        if (EconomyAPI.get().has(uuid, 10000)) {
            System.out.println("Người chơi có đủ tiền!");
        }
    }
}
```

## 2. Làm sao để tải API của VNEconomy trong plugin khác?

Có 2 cách:

### 🟢 Cách 1: Dùng plugin VNEconomy trực tiếp

Vì mình đã làm API bên trong plugin VNEconomy.jar, bạn không cần tải gì thêm.

Chỉ cần:

Thả `VNEconomy.jar` vào plugins/

Trong plugin.yml của plugin bạn, thêm:

`depend: [VNEconomy]`


hoặc nếu không bắt buộc thì:

`softdepend: [VNEconomy]`


Trong code, `import class từ org.fox.vneconomy.api.`

👉 Cách này dễ nhất vì bạn không phải build thêm file API riêng.

### 🟡 Cách 2: Dùng API Thư viện

Nếu bạn muốn build plugin mà không phụ thuộc JAR runtime, bạn có thể copy file VNEconomy-API.jar

Khi build plugin custom bằng Maven thêm đầy đủ api vào để tải thư viện api của plugin:

```yaml

<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.ShadowZa982</groupId>
    <artifactId>VNEconomy</artifactId>
    <version>master-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>

```