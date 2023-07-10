package io.github.projectpidove.showdown

import StatType.*

enum Nature(modifiedStats: Option[(StatType, StatType)] = None):

  def this(increased: StatType, decreased: StatType) = this(Some((increased, decreased)))

  // Neutral natures
  case Hardy
  case Docule
  case Serious
  case Bashful
  case Quirky

  // Modifying natures
  case Lonely extends Nature(Attack, Defense)
  case Brave extends Nature(Attack, Speed)
  case Adamant extends Nature(Attack, SpecialAttack)
  case Naughty extends Nature(Attack, SpecialDefense)
  case Bold extends Nature(Defense, Attack)
  case Relaxed extends Nature(Defense, Attack)
  case Impish extends Nature(Defense, SpecialAttack)
  case Lax extends Nature(Defense, SpecialDefense)
  case Timid extends Nature(Speed, Attack)
  case Hasty extends Nature(Speed, Defense)
  case Jolly extends Nature(Speed, SpecialAttack)
  case Naitve extends Nature(Speed, SpecialDefense)
  case Modest extends Nature(SpecialAttack, Attack)
  case Mild extends Nature(SpecialAttack, Defense)
  case Quiet extends Nature(SpecialAttack, Speed)
  case Rash extends Nature(SpecialAttack, SpecialDefense)
  case Calm extends Nature(SpecialDefense, Attack)
  case Gentle extends Nature(SpecialDefense, Defense)
  case Sassy extends Nature(SpecialDefense, Speed)
  case Careful extends Nature(SpecialDefense, SpecialAttack)
