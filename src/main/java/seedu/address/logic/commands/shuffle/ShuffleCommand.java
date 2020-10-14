package seedu.address.logic.commands.shuffle;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TRANSLATION;
import static seedu.address.logic.parser.CliSyntax.PREFIX_WORD;

import seedu.address.commons.core.Messages;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.entry.AddCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.deck.entry.Entry;

public class ShuffleCommand extends Command {

    public static final String COMMAND_WORD = "shuffle";
    public static final String MESSAGE_SUCCESS = "The current deck has been shuffled!";


    @Override
    public CommandResult execute(Model model) throws CommandException{
        requireNonNull(model);

        if (model.getCurrentDeck() == null) {
            throw new CommandException(Messages.MESSAGE_NO_DECK_SELECTED);
        }
        model.shuffleBank();
        return new CommandResult(MESSAGE_SUCCESS);
    }

}
