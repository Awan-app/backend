package com.ezdo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// Note: Only one goal proposal block should be provided in the AI response.

/*
 * {
 *   "type": "proposal",
 *   "proposal": {
 *
 *   }
 * }
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GoalProposalBlock extends ContentBlock {

    private GoalProposal proposal;

    @Override
    public String getType() {
        return "proposal";
    }
}
