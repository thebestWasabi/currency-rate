package ru.khamzin.tgbot.service;

import ch.qos.logback.core.status.StatusUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.khamzin.tgbot.dto.ValuteCursOnDate;
import ru.khamzin.tgbot.entity.ActiveChat;
import ru.khamzin.tgbot.repository.ActiveChatRepository;

import javax.annotation.PostConstruct;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class BotService extends TelegramLongPollingBot {
    private static final String CURRENT_RATES = "/currentrates";

    private final CentralRussianBankService centralRussianBankService;
    private final ActiveChatRepository activeChatRepository;

    @Value("${bot.name}")
    private String name;

    @Value("${bot.api.key}")
    private String apiKey;

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return apiKey;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();           //Этой строчкой мы получаем сообщение от пользователя
        try {
            SendMessage response = new SendMessage();    //Данный класс представляет собой реализацию команды отправки сообщения, которую за нас выполнит ранее подключенная библиотека
            Long chatId = message.getChatId();           //ID чата, в который необходимо отправить ответ
            response.setChatId(String.valueOf(chatId));  //Устанавливаем ID, полученный из предыдущего этап сюда, чтобы сообщить, в какой чат необходимо отправить сообщение

            //Тут начинается самое интересное - мы сравниваем, что прислал пользователь, и какие команды мы можем обработать. Пока что у нас только одна команда
            if (CURRENT_RATES.equalsIgnoreCase(message.getText())) {
                                                        //Получаем все курсы валют на текущий момент и проходимся по ним в цикле
                for (ValuteCursOnDate valuteCursOnDate : centralRussianBankService.getCurrenciesFromCbr()) {
                                                         //В данной строчке мы собираем наше текстовое сообщение
                                                         //StringUtils.defaultBlank – это метод из библиотеки Apache Commons, который нам нужен для того, чтобы на первой итерации нашего цикла была вставлена пустая строка вместо null, а на следующих итерациях не перетерся текст, полученный из предыдущих итерации. Подключение библиотеки см. ниже
                    response.setText(StringUtils.defaultIfBlank(response.getText(), "") + valuteCursOnDate.getName() + " - " + valuteCursOnDate.getCourse() + "\n");
                }
            }
            //Теперь мы сообщаем, что пора бы и ответ отправлять
            execute(response);
            if (activeChatRepository.findActiveChatByChatId(chatId).isEmpty()) {
                ActiveChat activeChat = new ActiveChat();
                activeChat.setChatId(chatId);
                activeChatRepository.save(activeChat);
            }
            //Ниже очень примитивная обработка исключений, чуть позже мы это поправим
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendNotificationToAllActiveChats(String message, Set<Long> chatIds) {
        for (Long id : chatIds) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(id));
            sendMessage.setText(message);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @PostConstruct
    public void start() {
        log.info("username: {}, token: {}", name, apiKey);
    }
}