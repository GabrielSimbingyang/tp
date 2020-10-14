package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.index.Index;
import seedu.address.model.deck.Deck;
import seedu.address.model.deck.DeckName;
import seedu.address.model.deck.entry.Entry;
import seedu.address.model.deck.entry.Translation;
import seedu.address.model.deck.entry.UniqueEntryList;
import seedu.address.model.deck.entry.Word;


/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    /*private final FilteredList<Entry> filteredEntries;*/
    private final FilteredList<Deck> filteredDecks;
    private Optional<Index> currentDeckIndex;

    private Deck observedDeck;


    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        super();
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        /*filteredEntries = new FilteredList<>(this.addressBook.getEntryList());*/
        filteredDecks = new FilteredList<>(this.addressBook.getDeckList());
        currentDeckIndex = Optional.empty();
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    //=========== Word Bank ================================================================================
    /**
     * Shuffles the observed entry list using a similar process as the replace entry list command except that the deck
     * is randomised
     * */
    @Override
    public void shuffleBank() {
        if (currentDeckIndex.equals(Optional.empty()) || currentDeckIndex.equals(0)) {
            logger.info("Deck not selected, cannot shuffle");
        }
        ObservableList<Entry> shuffledList = getCurrentDeck().getEntries().shuffle();
        Iterator<Entry> iterator = addressBook.getObservedEntries().iterator();
        ArrayList<Entry> copy = new ArrayList<Entry>();
        ArrayList<Word> questions = new ArrayList<>();
        ArrayList<Translation> answers = new ArrayList<>();

        while (iterator.hasNext()) {
            copy.add(iterator.next());
        }
        for (Entry entry : copy) {
            addressBook.getObservedEntries().remove(entry);
        }
        for (Entry entry : shuffledList) {
            questions.add(entry.getWord());
            answers.add(entry.getTranslation());
            addressBook.getObservedEntries().add(new Entry(entry.getWord(),new Translation("?")));
        }
        System.out.println(questions);
        System.out.println(answers);
    }

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasEntry(Entry entry) {
        requireNonNull(entry);
        Deck currentDeck = getCurrentDeck();
        return currentDeck.hasEntry(entry);
    }

    @Override
    public void deleteEntry(Entry target) {
        Deck currentDeck = getCurrentDeck();
        currentDeck.removeEntry(target);
        addressBook.getObservedEntries().remove(target);
    }

    /**
     * This function takes the entry and adds it to the deck entry list as well as the observedEntries in the
     * AddressBook
     *
     * @param entry refers to the entry inputted by the user
     */

    @Override
    public void addEntry(Entry entry) {
        Deck currentDeck = getCurrentDeck();
        currentDeck.addEntry(entry);
        addressBook.getObservedEntries().add(entry);
        updateFilteredEntryList(PREDICATE_SHOW_ALL_ENTRIES);
    }

    @Override
    public void setEntry(Entry target, Entry editedEntry) {
        requireAllNonNull(target, editedEntry);
        Deck currentDeck = getCurrentDeck();
        currentDeck.setEntry(target, editedEntry);
    }

    @Override
    public boolean hasDeck(Deck deck) {
        requireNonNull(deck);
        return addressBook.hasDeck(deck);
    }

    @Override
    public void removeDeck(Deck target) {
        addressBook.removeDeck(target);
    }


    @Override
    public void addDeck(Deck deck) {
        addressBook.addDeck(deck);
        updateFilteredDeckList(PREDICATE_SHOW_ALL_DECKS);
    }

    @Override
    public void selectDeck(Index index) {
        currentDeckIndex = Optional.of(index);
        //selectCommand.execute also called the function (below) replaceEntryList()
    }

    /**
     * This function deletes what is on the GUI and replaces it with the next entries in the selected deck.
     * To replace the observedEntry in Addressbook.java that controls the GUI, a copy of it has to be created first.
     * This avoids the concurrent modification exception.
     */
    @Override
    public void replaceEntryList() {
        UniqueEntryList observedList = getCurrentDeck().getEntries(); //get selected deck
        Iterator<Entry> iterator = addressBook.getObservedEntries().iterator(); //create iterator
        ArrayList<Entry> copy = new ArrayList<Entry>(); //initialise a copy
        while (iterator.hasNext()) { //fill the empty copy ArrayList with the existing entries
            copy.add(iterator.next()); //this avoids the concurrentModification exception
        }
        for (Entry entry : copy) { //for each entry in the copy, delete the same entry in the observedEntries
            addressBook.getObservedEntries().remove(entry); //this changes the GUI
        }

        for (Entry entry : observedList) { //for each entry in the new selected deck entryList
            addressBook.getObservedEntries().add(entry); //add it to the GUI
        }
        // note: you can use a void function to change the ui apparently
    }

    @Override
    public void clearEntryList() {
        Iterator<Entry> iterator = addressBook.getObservedEntries().iterator();
        ArrayList<Entry> copy = new ArrayList<>();
        while (iterator.hasNext()) {
            copy.add(iterator.next());
        }
        for (Entry entry : copy) {
            addressBook.getObservedEntries().remove(entry);
        }
    }

    @Override
    public Deck getCurrentDeck() {
        if (currentDeckIndex.equals(Optional.empty())) {
            logger.info("currentDeckIndex is 0");
            return null;
        }
        return filteredDecks.get(currentDeckIndex.get().getZeroBased());
    }

    //=========== Filtered Entry List Accessors =============================================================

    /**
     * Returns a default deck as memory is not fixed yet. During initialisation, the observedEntryList value is
     * passed as the AddressBook.javas uniqueEntryList. I.e the GUI now watches for any changes in the AddressBook,java
     * field observedEntries
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Entry> getFilteredEntryList() {
        //keeps returning null causing null error
        if (this.getCurrentDeck() == null) {
            logger.info("Current Deck is null");
            observedDeck = new Deck(new DeckName("Japanese(Built In Stub)"));
            observedDeck.addEntry(new Entry(new Word("Hello"), new Translation("こんにちは")));
            observedDeck.addEntry(new Entry(new Word("Goodbye"), new Translation("さようなら")));
            observedDeck.addEntry(new Entry(new Word("Software"), new Translation("ソフトウェア")));
            observedDeck.addEntry(new Entry(new Word("Engineering"), new Translation("エンジニアリング")));
            addressBook.addDeck(observedDeck);
            return addressBook.getFilteredEntries();
        }
        Deck currentDeck = getCurrentDeck();
        System.out.println(currentDeck.getDeckName());
        System.out.println(currentDeck.getEntryList());
        return getCurrentDeck().getFilteredEntryList();
    }

    @Override
    public void updateFilteredEntryList(Predicate<Entry> predicate) {
        requireNonNull(predicate);
        Deck currentDeck = getCurrentDeck();
        currentDeck.updateFilteredEntryList(predicate);

        Iterator<Entry> iterator = addressBook.getObservedEntries().iterator(); //create iterator
        ArrayList<Entry> copy = new ArrayList<Entry>(); //initialise a copy
        while (iterator.hasNext()) { //fill the empty copy ArrayList with the existing entries
            copy.add(iterator.next()); //this avoids the concurrentModification exception
        }
        for (Entry entry : copy) { //for each entry in the copy, delete the same entry in the observedEntries
            addressBook.getObservedEntries().remove(entry); //this changes the GUI
        }

        for (Entry entry : currentDeck.getFilteredEntryList()) { //for each entry in filtered list
            addressBook.getObservedEntries().add(entry); //add it to the GUI
        }
    }

    /**
     * Returns an unmodifiable view of the list of {@code Deck} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Deck> getFilteredDeckList() {
        return filteredDecks;
    }

    @Override
    public void updateFilteredDeckList(Predicate<Deck> predicate) {
        requireNonNull(predicate);
        filteredDecks.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return addressBook.equals(other.addressBook)
                && userPrefs.equals(other.userPrefs)
                && filteredDecks.equals(other.filteredDecks);
    }


}
