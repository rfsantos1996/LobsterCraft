-- MySQL Script generated by MySQL Workbench
-- Dom 31 Jan 2016 19:25:00 BRST
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema minecraft
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema minecraft
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `minecraft` DEFAULT CHARACTER SET utf8 ;
USE `minecraft` ;

-- -----------------------------------------------------
-- Table `minecraft`.`user_profiles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`user_profiles` (
  `playerId` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `playerName` VARCHAR(16) NOT NULL,
  `password` VARCHAR(64) NOT NULL,
  `lastTimeOnline` BIGINT(19) UNSIGNED NOT NULL,
  `playTime` BIGINT(19) UNSIGNED NOT NULL,
  `lastIp` VARCHAR(16) NOT NULL,
  PRIMARY KEY (`playerId`),
  UNIQUE INDEX `playerId_UNIQUE` (`playerId` ASC),
  UNIQUE INDEX `playerNane_UNIQUE` (`playerName` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`ban_records`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`ban_records` (
  `recordId` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_playerId` BIGINT(19) UNSIGNED NOT NULL,
  `responsibleId` BIGINT(19) UNSIGNED NOT NULL,
  `banType` TINYINT NOT NULL,
  `recordDate` BIGINT(19) UNSIGNED NOT NULL,
  `reason` VARCHAR(120) NOT NULL,
  `unbanDate` BIGINT(19) UNSIGNED NULL,
  INDEX `fk_ban_records_user_profiles_idx` (`user_playerId` ASC),
  PRIMARY KEY (`recordId`, `user_playerId`),
  UNIQUE INDEX `recordId_UNIQUE` (`recordId` ASC),
  INDEX `fk_ban_records_user_profiles1_idx` (`responsibleId` ASC),
  CONSTRAINT `fk_ban_records_user_profiles`
    FOREIGN KEY (`user_playerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_ban_records_user_profiles1`
    FOREIGN KEY (`responsibleId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`worlds`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`worlds` (
  `worldId` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `worldName` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`worldId`),
  UNIQUE INDEX `worldId_UNIQUE` (`worldId` ASC),
  UNIQUE INDEX `worldName_UNIQUE` (`worldName` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`last_location_profile`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`last_location_profile` (
  `user_playerId` BIGINT(19) UNSIGNED NOT NULL,
  `worldId` BIGINT(19) UNSIGNED NULL DEFAULT NULL,
  `x` DOUBLE NULL DEFAULT NULL,
  `y` DOUBLE NULL DEFAULT NULL,
  `z` DOUBLE NULL DEFAULT NULL,
  `yaw` FLOAT NULL DEFAULT NULL,
  `pitch` FLOAT NULL DEFAULT NULL,
  PRIMARY KEY (`user_playerId`),
  UNIQUE INDEX `user_playerId_UNIQUE` (`user_playerId` ASC),
  INDEX `fk_last_location_profile_worlds1_idx` (`worldId` ASC),
  CONSTRAINT `fk_location_profile_user_profiles1`
    FOREIGN KEY (`user_playerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_last_location_profile_worlds1`
    FOREIGN KEY (`worldId`)
    REFERENCES `minecraft`.`worlds` (`worldId`)
    ON DELETE SET NULL
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`recent_username_changes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`recent_username_changes` (
  `user_playerId` BIGINT(19) UNSIGNED NOT NULL,
  `oldPlayerName` VARCHAR(16) NOT NULL,
  `changeDate` BIGINT(19) UNSIGNED NOT NULL,
  PRIMARY KEY (`user_playerId`),
  UNIQUE INDEX `oldPlayerName_UNIQUE` (`oldPlayerName` ASC),
  UNIQUE INDEX `user_playerId_UNIQUE` (`user_playerId` ASC),
  CONSTRAINT `fk_recent_username_changes_user_profiles1`
    FOREIGN KEY (`user_playerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`muted_players`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`muted_players` (
  `mute_index` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_mutedId` BIGINT(19) UNSIGNED NOT NULL,
  `user_ownerId` BIGINT(19) UNSIGNED NOT NULL,
  `muteDate` BIGINT(19) UNSIGNED NOT NULL,
  UNIQUE INDEX `mutedId_UNIQUE` (`mute_index` ASC),
  PRIMARY KEY (`mute_index`, `user_mutedId`),
  INDEX `fk_muted_players_user_profiles1_idx` (`user_mutedId` ASC),
  INDEX `fk_muted_players_user_profiles2_idx` (`user_ownerId` ASC),
  CONSTRAINT `fk_muted_players_user_profiles1`
    FOREIGN KEY (`user_mutedId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_muted_players_user_profiles2`
    FOREIGN KEY (`user_ownerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`mod_muted_players`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`mod_muted_players` (
  `mute_index` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_mutedId` BIGINT(19) UNSIGNED NOT NULL,
  `user_moderatorId` BIGINT(19) UNSIGNED NOT NULL,
  `muteDate` BIGINT(19) UNSIGNED NOT NULL,
  `unmuteDate` BIGINT(19) UNSIGNED NOT NULL,
  `reason` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`mute_index`, `user_mutedId`),
  INDEX `fk_mod_muted_players_user_profiles1_idx` (`user_mutedId` ASC),
  INDEX `fk_mod_muted_players_user_profiles2_idx` (`user_moderatorId` ASC),
  UNIQUE INDEX `mute_index_UNIQUE` (`mute_index` ASC),
  CONSTRAINT `fk_mod_muted_players_user_profiles1`
    FOREIGN KEY (`user_mutedId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_mod_muted_players_user_profiles2`
    FOREIGN KEY (`user_moderatorId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`world_blocks`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`world_blocks` (
  `worlds_worldId` BIGINT(19) UNSIGNED NOT NULL,
  `chunkX` MEDIUMINT NOT NULL,
  `chunkZ` MEDIUMINT NOT NULL,
  `blockX` TINYINT UNSIGNED NOT NULL,
  `blockY` TINYINT UNSIGNED NOT NULL,
  `blockZ` TINYINT UNSIGNED NOT NULL,
  `user_ownerId` BIGINT(19) UNSIGNED NOT NULL,
  PRIMARY KEY (`worlds_worldId`, `chunkX`, `chunkZ`),
  UNIQUE INDEX `UNIQUE` (`worlds_worldId` ASC, `chunkX` ASC, `chunkZ` ASC, `blockX` ASC, `blockY` ASC, `blockZ` ASC),
  INDEX `fk_table1_user_profiles1_idx` (`user_ownerId` ASC),
  CONSTRAINT `fk_table1_worlds1`
    FOREIGN KEY (`worlds_worldId`)
    REFERENCES `minecraft`.`worlds` (`worldId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_table1_user_profiles1`
    FOREIGN KEY (`user_ownerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`player_inventories`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`player_inventories` (
  `user_playerId` BIGINT(19) UNSIGNED NOT NULL,
  `contents` BLOB NOT NULL,
  `armor_contents` BLOB NOT NULL,
  `remaining_contents` BLOB NULL,
  PRIMARY KEY (`user_playerId`),
  CONSTRAINT `fk_table1_user_profiles2`
    FOREIGN KEY (`user_playerId`)
    REFERENCES `minecraft`.`user_profiles` (`playerId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`world_constructions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`world_constructions` (
  `constructionId` BIGINT(19) UNSIGNED NOT NULL AUTO_INCREMENT,
  `constructionName` VARCHAR(45) NOT NULL,
  UNIQUE INDEX `constructionId_UNIQUE` (`constructionId` ASC, `constructionName` ASC),
  PRIMARY KEY (`constructionId`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `minecraft`.`world_admin_blocks`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`world_admin_blocks` (
  `constructionId` BIGINT(19) UNSIGNED NOT NULL,
  `worlds_worldId` BIGINT(19) UNSIGNED NOT NULL,
  `chunkX` MEDIUMINT NOT NULL,
  `chunkZ` MEDIUMINT NOT NULL,
  `blockX` TINYINT UNSIGNED NOT NULL,
  `blockY` TINYINT UNSIGNED NOT NULL,
  `blockZ` TINYINT UNSIGNED NOT NULL,
  UNIQUE INDEX `UNIQUE` (`worlds_worldId` ASC, `chunkX` ASC, `chunkZ` ASC, `blockX` ASC, `blockY` ASC, `blockZ` ASC),
  INDEX `fk_world_admin_blocks_world_constructions1_idx` (`constructionId` ASC),
  INDEX `fk_world_admin_blocks_worlds1_idx` (`worlds_worldId` ASC),
  CONSTRAINT `fk_world_admin_blocks_world_constructions1`
    FOREIGN KEY (`constructionId`)
    REFERENCES `minecraft`.`world_constructions` (`constructionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_world_admin_blocks_worlds1`
    FOREIGN KEY (`worlds_worldId`)
    REFERENCES `minecraft`.`worlds` (`worldId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
