package cloneproject.Instagram.domain.dm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndicateDTO {

    private Long roomId;
    private Long senderId;
    private Long ttl;
}
