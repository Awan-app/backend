package com.ezdo.dto.ai.decompose;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/*
* {
*   "type": "text",
*   "text": "..."
* }
* */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TextBlock extends ContentBlock {

    private String text;

    @Override
    public String getType() {
        return "text";
    }
}
