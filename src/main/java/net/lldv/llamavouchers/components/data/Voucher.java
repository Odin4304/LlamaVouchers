package net.lldv.llamavouchers.components.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class Voucher {

    private final String id;
    private final String player;
    private final int uses;
    private final List<String> players;
    private final long time;
    private final List<String> rewards;

}
