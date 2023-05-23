package ru.khamzin.tgbot.entity;


import javax.persistence.*;
import java.math.BigDecimal;

@Entity
public class Spend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "spend")
    private BigDecimal spend;
}
