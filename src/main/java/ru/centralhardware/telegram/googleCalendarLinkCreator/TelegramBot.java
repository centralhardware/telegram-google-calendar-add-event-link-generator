package ru.centralhardware.telegram.googleCalendarLinkCreator;

import com.fasterxml.jackson.databind.SequenceWriter;
import org.apache.http.client.utils.URIBuilder;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TelegramBot extends TelegramLongPollingBot {

    private final Map<Long, Detail> longDetailMap = new HashMap<>();
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd'T'Hm00");

    static {
        ApiContextInitializer.init();
    }

    public static TelegramBot initTelegramBot(){
        TelegramBot absSender = null;
        try {
            absSender = new TelegramBot();
            TelegramBotsApi botsApi = new TelegramBotsApi();
            botsApi.registerBot(absSender);
        } catch (TelegramApiRequestException e) {
            System.exit(1);
        }
        return absSender;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()){
                String message = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                if (message.equals("/start")){
                    execute(new SendMessage().setChatId(chatId).setText("""
            Бот позволяет создать ссылку по нажатию на которую google предложить нажавшему добавить в его google 
            calendar событие которые вы задатие при создание ссылки
            """));
                } else if (message.equals("/add")){
                    execute(new SendMessage().setChatId(chatId).setText("Введите название вашего мероприятия"));
                    longDetailMap.put(chatId, new Detail());
                    longDetailMap.get(chatId).steps = Steps.INPUT_NAME;
                } else {
                    if (longDetailMap.containsKey(chatId)){
                        switch (longDetailMap.get(chatId).steps){
                            case INPUT_NAME -> {
                                longDetailMap.get(chatId).event.name = message;
                                longDetailMap.get(chatId).steps = Steps.INPUT_DATE;
                                execute(new SendMessage().setChatId(chatId).setText("Введите дату начала в формате H:m dd-MM-yyyy"));
                            }
                            case INPUT_DATE -> {
                                try {
                                    Date date = new SimpleDateFormat("H:m dd-MM-yyyy").parse(message);
                                    longDetailMap.get(chatId).event.date = date;
                                    longDetailMap.get(chatId).steps = Steps.INPUT_DETAIL;
                                    execute(new SendMessage().setChatId(chatId).setText("Введите описание"));
                                } catch (ParseException e) {
                                    execute(new SendMessage().setChatId(chatId).setText("Введите дату начала в формате H:m dd-MM-yyyy"));
                                }
                            }
                            case INPUT_DETAIL -> {
                                longDetailMap.get(chatId).event.detail = message;
                                longDetailMap.get(chatId).steps = Steps.INPUT_LOCATION;
                                execute(new SendMessage().setChatId(chatId).setText("Введите где будет проходить событие"));
                            }
                            case INPUT_LOCATION -> {
                                longDetailMap.get(chatId).event.location = message;
                                Event event = longDetailMap.get(chatId).event;
                                String date = dateFormatter.format(event.date);
                                if (date.contains("000")){
                                    date+="0";
                                }
                                execute(new SendMessage().
                                        setChatId(chatId).
                                        setText(new URIBuilder().
                                                setScheme("https").
                                                setHost("calendar.google.com").
                                                setPath("calendar/render").
                                                setParameter("action", "TEMPLATE").
                                                setParameter("text", event.name).
                                                setParameter("dates", String.format("%s/%s",
                                                        date,
                                                        date)).
                                                setParameter("location", event.location).
                                                setParameter("crm", "BUSY").toString()));
                            }
                        }
                    }
                }
                }
            } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public String getBotToken() {
        return "";
    }

}
