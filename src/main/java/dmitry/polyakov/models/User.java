package dmitry.polyakov.models;

import dmitry.polyakov.constants.BotStateEnum;
import dmitry.polyakov.constants.SettingsOrderEnum;
import jakarta.transaction.Transactional;
import lombok.*;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Transactional
@AllArgsConstructor
@Table(name = "users", schema = "telegram")
public class User {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "registered_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp registeredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_bot_state")
    private BotStateEnum userBotState;

    @ManyToMany(mappedBy = "users", cascade = CascadeType.DETACH)
    private Set<Phrase> phrases = new HashSet<>();

    @Column(name = "current_page_number")
    private int currentPageNumber;

    @Column(name = "current_phrase")
    private String currentPhrase;

    @Column(name = "language")
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "phrase_sorting_state")
    private SettingsOrderEnum phraseSortingState;

    @Column(name = "phrases_per_page")
    private int phrasesPerPage;
}
