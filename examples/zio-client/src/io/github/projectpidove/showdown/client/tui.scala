package io.github.projectpidove.showdown.client

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive
import io.github.projectpidove.showdown.battle.*
import io.github.projectpidove.showdown.protocol.server.choice.*

/**
 * Pretty print a team member.
 *
 * @param member the member to represent
 * @return a textual representation of the given member
 */
def showTeamMember(member: TeamMember): String =
  val species = member.details.species
  val item = member.item match
    case HeldItem.Revealed(item, Some(cause)) => s"@ $item (Revealed by $cause})"
    case HeldItem.Revealed(item, None) => s"@ $item"
    case HeldItem.Destroyed(item, Some(cause)) => s"@ $item (Destroyed by $cause})"
    case HeldItem.Destroyed(item, None) => s"@ $item"
    case HeldItem.Unknown => ""

  val condition = member.condition match
    case Condition(Health(current, max), Some(status)) => s"$current/$max $status"
    case Condition(Health(current, max), None) => s"$current/$max"

  val ability = member.ability.fold("Unknown ability")(_.value)

  s"$species $item, $ability, $condition"

/**
 * Pretty print a team.
 *
 * @param player the team's owner
 * @param team the team to represent
 * @return a textual representation of the given team
 */
def showTeam(player: PlayerNumber, team: PlayerTeam): String =
  val memberInfo =
    team
      .members
      .map((slot, member) => s"$slot: ${showTeamMember(member)}")

  val remainingLines = Range(team.members.size, team.size.value).map(slot => s"$slot:")

  s"""=== Player $player ===
     |${(memberInfo ++ remainingLines).mkString("\n")}""".stripMargin

/**
 * Pretty print an active pokemon.
 *
 * @param position the position of the pokemon
 * @param pokemon the pokemon to represent
 * @param member the team info of the pokemon
 * @return a textual representation of the given pokemon
 */
def showActive(position: ActivePosition, pokemon: ActivePokemon, member: TeamMember): String =
  val species = pokemon.transformedSpecies match
    case Some(transformed) => s"$transformed (${member.details.species})"
    case None => member.details.species

  val item = member.item match
    case HeldItem.Revealed(item, Some(cause)) => s"@ $item (Revealed by $cause})"
    case HeldItem.Revealed(item, None) => s"@ $item"
    case HeldItem.Destroyed(item, Some(cause)) => s"@ $item (Destroyed by $cause})"
    case HeldItem.Destroyed(item, None) => s"@ $item"
    case HeldItem.Unknown => ""

  val condition = member.condition match
    case Condition(Health(current, max), Some(status)) => s"$current/$max $status"
    case Condition(Health(current, max), None) => s"$current/$max"

  val baseAbility = member.ability.fold("Unknown Ability")(_.value)

  val ability = pokemon.modifiedAbility match
    case Some(RevealedAbility.Base(ability)) => ability
    case Some(RevealedAbility.Modified(ability, _)) => s"$ability ($baseAbility)"
    case Some(RevealedAbility.Disabled) => s"Disabled ($baseAbility)"
    case None => baseAbility

  s"""=== $position ===
     |$species $item
     |Condition: $condition
     |Ability: $ability""".stripMargin

/**
 * Pretty print a all active pokemon of a battle.
 *
 * @param battle the battle containing the pokemon to represent
 * @return a textual representation of the active pokemon of the given battle
 */
def showAllActive(battle: Battle): String =
  val entries =
    battle.activePokemon.map: (position, pokemon) =>
      battle.getTeamMemberAt(position) match
        case Some(value) => showActive(position, pokemon, value)
        case None =>
          s"""=== $position ===
             |???""".stripMargin

  entries.mkString("\n")

/**
 * Pretty print a move choice.
 *
 * @param choice the move to represent
 * @return a textual representation of the given usable move
 */
def showMoveChoice(choice: MoveChoice): String = s"${choice.name} (${choice.pp}/${choice.maxPP})"

/**
 * Pretty print a pokemon choice.
 *
 * @param choice the pokemon to represent
 * @return a textual representation of the given switchable pokemon
 */
def showPokemonChoice(choice: PokemonChoice): String =
  val condition = choice.condition match
    case Condition(Health(current, max), Some(status)) => s"$current/$max $status"
    case Condition(Health(current, max), None) => s"$current/$max"

  s"${choice.details.species} ($condition)"

/**
 * Pretty print the choices of an active pokemon.
 *
 * @param choice the moves to represent
 * @return a textual representation of the given active choice
 */
def showActiveChoice(choice: ActiveChoice): String =
  choice.moves.map(showMoveChoice).mkString(" / ")

/**
 * Pretty print a team choice.
 *
 * @param choice the team to represent
 * @return a textual representation of the switch options
 */
def showTeamChoice(choice: TeamChoice): String =
  choice.pokemon.map(showPokemonChoice).mkString(" / ")

/**
 * Pretty print a choice request.
 *
 * @param choice the choice to represent
 * @return a textual representation of the given choice.
 */
def showChoiceRequest(choice: ChoiceRequest): String =
  val activeChoices = choice.active.map(showActiveChoice).mkString("\n")

  s"""Moves:
     |$activeChoices
     |
     |Switch:
     |${showTeamChoice(choice.team)}""".stripMargin

/**
 * Colorize a text.
 *
 * @param text the text to colorize
 * @param color the color to apply
 * @return a new String colored with the given ANSI color
 */
def colored(text: String, color: String): String =
  s"$color$text${Console.RESET}"

/**
 * Pretty print opponent's team, highlighting active and fainted pokemon.
 *
 * @param player the team's owner
 * @param team the team to render
 * @param active the set of slots of active pokemon to highlight
 * @return a textual representation of the given team
 */
def showOpponentTeam(player: PlayerNumber, team: PlayerTeam, active: Set[TeamSlot]): String =
  val members =
    team.members.map: (slot, pokemon) =>

      val condition = pokemon.condition
      val label =
        if condition.status.isDefined then s"${pokemon.details.species} (${condition.health.current}/${condition.health.max} ${condition.status.get})"
        else s"${pokemon.details.species} (${condition.health.current}/${condition.health.max})"

      if active.contains(slot) then colored(label, Console.GREEN)
      else if pokemon.condition.fainted then colored(label, Console.RED)
      else label

  s"Player $player: ${members.mkString(" / ")}"

/**
 * Pretty print battle's current state.
 *
 * @param battle the battle state to show
 * @param choice the choice to render
 * @return a textual representation of the given battle state and choice request
 */
def showBattleState(battle: Battle, choice: ChoiceRequest): String =
  val opponents = battle.players.filterNot(_._1 == PlayerNumber(1)).map: (n, player) =>
    player.team match
      case Some(team) =>
        val active =
          battle.activePokemon.collect:
            case (ActivePosition(playerNumber, _), pokemon) if playerNumber == n => pokemon.teamSlot
        showOpponentTeam(n, team, active.toSet)
      case None => s"=== $n ===\n???"

  s"""${opponents.mkString("\n")}
    |
    |${showChoiceRequest(choice)}""".stripMargin