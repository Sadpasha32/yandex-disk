package disk.api;

import disk.SystemItem;
import lombok.Data;

import java.util.List;
@Data
public class SystemItemRequest {
    private List<SystemItem> items;
    private String updateDate;
}
