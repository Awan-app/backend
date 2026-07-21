package com.ezdo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * {
 *   "type": "question",
 *   "text": "...",
 *   "options": ["option 1", "option 2", "option 3"]
 * }
 * */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class QuestionBlock extends ContentBlock {

    private String text;
    private List<String> options;

    @Override
    public String getType() {
        return "question";
    }
}
