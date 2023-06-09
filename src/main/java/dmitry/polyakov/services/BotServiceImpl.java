package dmitry.polyakov.services;

import com.vdurmont.emoji.EmojiParser;
import dmitry.polyakov.bot.PersonalVocabularyBot;
import dmitry.polyakov.exceptions.PhraseNotFoundException;
import dmitry.polyakov.exceptions.UserNotFoundException;
import dmitry.polyakov.handlers.CallbackHandler;
import dmitry.polyakov.handlers.MessageHandler;
import dmitry.polyakov.models.User;
import dmitry.polyakov.utils.LanguageLocalisation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ResourceBundle;

import static dmitry.polyakov.constants.BotStateEnum.*;

@Slf4j
@Service
public class BotServiceImpl implements BotService {
    private final MessageHandler commandHandler;
    private final CallbackHandler callbackHandler;
    private final UserService userService;
    private final UserPhraseService userPhraseService;
    private final LanguageLocalisation languageLocalisation;

    @Autowired
    public BotServiceImpl(MessageHandler commandHandler,
                          CallbackHandler callbackHandler,
                          UserService userService,
                          UserPhraseService userPhraseService,
                          LanguageLocalisation languageLocalisation) {
        this.commandHandler = commandHandler;
        this.callbackHandler = callbackHandler;
        this.userService = userService;
        this.languageLocalisation = languageLocalisation;
        this.userPhraseService = userPhraseService;
    }

    @Override
    public void getUpdate(Update update, PersonalVocabularyBot bot) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            try {
                handleMessages(update, chatId, text, bot);
            } catch (UserNotFoundException e) {
                log.warn("Error finding user with id = " + chatId, e);
            } catch (PhraseNotFoundException e) {
                log.warn("Error fetching the phrase with this context: " + text, e);
            } catch (TelegramApiException e) {
                log.warn("Unexpected error has occurred while processing", e);
            }
        } else if (update.hasCallbackQuery()) {
            try {
                handleCallback(update, bot);
            } catch (TelegramApiException e) {
                log.warn("Unexpected error has occurred while processing", e);
            } catch (UserNotFoundException e) {
                log.warn("Error finding user with id = " + update.getMessage().getChatId(), e);
            }

        }
    }

    private void handleMessages(Update update, Long chatId, String command, PersonalVocabularyBot bot) throws UserNotFoundException, PhraseNotFoundException, TelegramApiException {
        ResourceBundle messages = languageLocalisation.getMessages(chatId);

        if (command.equals("/start")) {
            commandHandler.handleStartCommandReceived(update, chatId, bot);
        }

        User user = userService.findUserById(chatId);

        if (command.equals("/help")) {
            user.setUserBotState(DEFAULT_STATE);
            userService.saveUser(user);

            commandHandler.handleHelpCommandReceived(update, chatId, bot);

        } else if (command.equals(EmojiParser.parseToUnicode(messages.getString("button.name.dictionary")
                + ":scroll:"))
                && user.getUserBotState().equals(DEFAULT_STATE)) {
            user.setUserBotState(READING_DICTIONARY);
            userService.saveUser(user);

            commandHandler.handleDictionaryCommandReceived(chatId, bot);

        } else if (command.equals("/language")) {
            user.setUserBotState(LANGUAGE_CHANGE);
            userService.saveUser(user);
            commandHandler.handleBotLanguageChange(update, chatId, bot);

        } else if (command.equals(EmojiParser.parseToUnicode(messages.getString("button.name.write")
                + ":writing:"))
                && user.getUserBotState().equals(DEFAULT_STATE)) {
            user.setUserBotState(WRITING_WORDS);
            userService.saveUser(user);

            commandHandler.handleWriteCommandReceived(update, chatId, bot);

        } else if (command.equals(EmojiParser.parseToUnicode(messages.getString("button.name.return")
                + ":house:")) && !user.getUserBotState().equals(DEFAULT_STATE)) {
            user.setUserBotState(DEFAULT_STATE);
            userService.saveUser(user);

            commandHandler.handleReturnButtonPressed(update, chatId, bot);

        } else if (command.equals(languageLocalisation.englishLang)
                || command.equals(languageLocalisation.russianLang)) {
            commandHandler.handleLanguageChange(update, chatId, command, bot);
            commandHandler.handleReturnButtonPressed(update, chatId, bot);

        } else if (user.getUserBotState().equals(WRITING_WORDS)) {
            commandHandler.handlePhraseReceived(update, chatId, command, bot);

        } else if (user.getUserBotState().equals(SETTINGS_AMOUNT)) {
            commandHandler.handlePhrasesNumberReceived(update, bot, chatId, command);
            callbackHandler.handleReturnToMenu(update, bot, chatId);
        }
    }

    private void handleCallback(Update update, PersonalVocabularyBot bot) throws TelegramApiException, UserNotFoundException {
        String callBackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        User user = userService.findUserById(chatId);

        switch (callBackData) {
            case "FAST_BACK_BUTTON" -> callbackHandler.handleFastBackButtonPressed(bot, chatId, messageId);

            case "BACK_BUTTON" -> callbackHandler.handleBackButtonPressed(bot, chatId, messageId);

            case "SETTINGS_BUTTON" -> callbackHandler.handleSettingsButtonPressed(bot, chatId, messageId);

            case "CANCEL_BUTTON" -> {
                if (user.getUserBotState().equals(READING_PHRASE)
                        || user.getUserBotState().equals(SETTINGS)) {
                    commandHandler.handleCancelButtonWhileReadingPhrasePressed(bot, chatId, messageId);
                    commandHandler.handleDictionaryCommandReceived(chatId, bot);
                } else
                    callbackHandler.handleCancelButtonPressed(update, bot, chatId, messageId);
            }

            case "SEARCHING_BUTTON" -> {
            }

            case "FORWARD_BUTTON" -> callbackHandler.handleForwardButtonPressed(bot, chatId, messageId);

            case "FAST_FORWARD_BUTTON" -> callbackHandler.handleFastForwardButtonPressed(bot, chatId, messageId);

            case "DELETE_BUTTON" -> callbackHandler.handleDeletePhraseButtonPressed(bot, chatId, messageId);

            case "YES_BUTTON" -> {
                commandHandler.deletePhrase(bot, chatId);
                callbackHandler.handlePhraseNumberPressed(bot, chatId, messageId, callBackData);
            }

            case "NO_BUTTON" -> callbackHandler.handleNOButtonPressed(bot, chatId, messageId);

            case "ORDER_BUTTON" -> callbackHandler.handleOrderButtonPressed(bot, chatId, messageId);

            case "AMOUNT_BUTTON" -> callbackHandler.handleAmountButtonPressed(bot, chatId, messageId);

            case "LEN_ASC_BUTTON", "LEN_DESC_BUTTON", "VIEWS_ASC_BUTTON", "VIEWS_DESC_BUTTON", "DATE_ASC_BUTTON", "DATE_DESC_BUTTON",
                    "ALPHABET_ASC_BUTTON", "ALPHABET_DESC_BUTTON"-> {
                callbackHandler.handleOrderOptionButtonPressed(callBackData, chatId);
                callbackHandler.handleCancelButtonPressed(update, bot, chatId, messageId);
            }
            case "ENGLISH_MEANINGS_BUTTON" -> commandHandler.handleEnglishMeaningsButtonPressed(bot, chatId, messageId);
            case "SENTENCES_BUTTON" -> commandHandler.handleSentencesButtonPressed(bot, chatId, messageId);

        }
        if (callBackData.matches("[0-9]+: [a-zA-Z'\\-, ]+")) {
            user.setCurrentPhrase(callBackData.split(": ")[1]);
            userService.saveUser(user);
            userPhraseService.incrementCountPhraseViews(chatId, callBackData.split(": ")[1]);
            callbackHandler.handlePhraseNumberPressed(bot, chatId, messageId, callBackData);
        }
    }
}
