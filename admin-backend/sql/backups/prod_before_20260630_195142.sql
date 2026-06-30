-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: 192.168.0.118    Database: ai_manager_admin
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ec_carton`
--

DROP TABLE IF EXISTS `ec_carton`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_carton` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绾哥涓婚敭',
  `factory_id` bigint DEFAULT NULL COMMENT '鎵€灞炲伐鍘?,
  `name` varchar(128) NOT NULL COMMENT '绾哥鍚嶇О',
  `length_cm` decimal(10,2) DEFAULT NULL COMMENT '闀?cm)',
  `width_cm` decimal(10,2) DEFAULT NULL COMMENT '瀹?cm)',
  `height_cm` decimal(10,2) DEFAULT NULL COMMENT '楂?cm)',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT '鍗曚环',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `illustration_variant` tinyint DEFAULT NULL COMMENT '绾哥鏉愯川 0~3锛堢墰鐨?鐧藉崱/鐡︽/鏅€氬揩閫掔洅锛?,
  `preview_image` varchar(256) DEFAULT NULL COMMENT '3D棰勮鍥炬枃浠跺悕',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_carton_factory` (`factory_id`),
  KEY `idx_ec_carton_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢绾哥';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_carton`
--

LOCK TABLES `ec_carton` WRITE;
/*!40000 ALTER TABLE `ec_carton` DISABLE KEYS */;
INSERT INTO `ec_carton` VALUES (1,1,'浜斿眰鐡︽澶栫-涓彿',42.00,32.00,28.00,3.50,'閫傜敤浜庨┈鍏嬫澂 24 瑁?,NULL,NULL,0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(2,2,'PP鏀剁撼涓撶敤绠?,62.00,42.00,38.00,4.20,'灏忓彿鏀剁撼鐩?20 瑁?,NULL,NULL,0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(3,3,'绔圭氦缁撮鍨僵绠?,48.00,34.00,26.00,2.80,'鍥涗欢濂楅鍨?30 瑁?,NULL,NULL,0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(4,2,'鍔犲帤鐗╂祦绠?澶у彿',68.00,48.00,42.00,5.60,'澶у彿鏀剁撼鐩?12 瑁咃紝鍔犲己鑰愬帇',NULL,NULL,0,'2026-06-24 15:38:23','2026-06-24 15:38:23');
/*!40000 ALTER TABLE `ec_carton` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_express_notice`
--

DROP TABLE IF EXISTS `ec_express_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_express_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '椤荤煡涓婚敭',
  `station_id` bigint NOT NULL COMMENT '绔欑偣ID',
  `content` text NOT NULL COMMENT '椤荤煡鍐呭',
  `highlight_red` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁鏍囩孩 1鏄?0鍚?,
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭锛岃秺灏忚秺闈犲墠',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_express_notice_station` (`station_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢蹇€掗』鐭?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_express_notice`
--

LOCK TABLES `ec_express_notice` WRITE;
/*!40000 ALTER TABLE `ec_express_notice` DISABLE KEYS */;
INSERT INTO `ec_express_notice` VALUES (1,1,'鐢熼矞銆佹槗纰庡搧璇锋彁鍓嶅憡鐭ュ鏈嶅苟鍔犲己鍖呰銆?,1,1,0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(2,1,'鍋忚繙鍦板尯鍙兘浜х敓闄勫姞璐圭敤锛屼互瀹為檯鎻芥敹涓哄噯銆?,0,2,0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(3,2,'涓€氫笉鏀寔娑蹭綋銆佺矇鏈瓑杩濈鍝佸瘎閫掋€?,1,1,0,'2026-06-24 15:38:25','2026-06-24 15:38:25');
/*!40000 ALTER TABLE `ec_express_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_express_price`
--

DROP TABLE IF EXISTS `ec_express_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_express_price` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浠锋牸涓婚敭',
  `station_id` bigint NOT NULL COMMENT '绔欑偣ID',
  `province_name` varchar(64) NOT NULL COMMENT '鐪佷唤鍚嶇О',
  `price_w03_kg` decimal(12,2) DEFAULT NULL COMMENT '0.3kg浠锋牸',
  `price_w05_kg` decimal(12,2) DEFAULT NULL COMMENT '0.5kg浠锋牸',
  `price_w1_kg` decimal(12,2) DEFAULT NULL COMMENT '1kg浠锋牸',
  `price_w15_kg` decimal(12,2) DEFAULT NULL COMMENT '1.5kg浠锋牸',
  `price_w2_kg` decimal(12,2) DEFAULT NULL COMMENT '2kg浠锋牸',
  `price_w25_kg` decimal(12,2) DEFAULT NULL COMMENT '2.5kg浠锋牸',
  `price_w3_kg` decimal(12,2) DEFAULT NULL COMMENT '3kg浠锋牸',
  `over3_first_price` decimal(12,2) DEFAULT NULL COMMENT '瓒?kg棣栭噸浠锋牸',
  `over3_additional_price` decimal(12,2) DEFAULT NULL COMMENT '瓒?kg缁噸浠锋牸',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_express_price_station_province` (`station_id`,`province_name`),
  KEY `idx_ec_express_price_station` (`station_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢蹇€掍环鏍?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_express_price`
--

LOCK TABLES `ec_express_price` WRITE;
/*!40000 ALTER TABLE `ec_express_price` DISABLE KEYS */;
INSERT INTO `ec_express_price` VALUES (1,1,'骞夸笢鐪?,12.00,14.00,18.00,22.00,26.00,30.00,34.00,34.00,6.00,0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(2,1,'鍖椾含甯?,15.00,17.00,22.00,26.00,30.00,34.00,38.00,38.00,8.00,0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(3,2,'骞夸笢鐪?,8.00,9.00,11.00,13.00,15.00,17.00,19.00,19.00,4.00,0,'2026-06-24 15:38:25','2026-06-24 15:38:25');
/*!40000 ALTER TABLE `ec_express_price` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_express_station`
--

DROP TABLE IF EXISTS `ec_express_station`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_express_station` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绔欑偣涓婚敭',
  `name` varchar(128) NOT NULL COMMENT '蹇€掑悕绉?,
  `avatar_url` varchar(256) DEFAULT NULL COMMENT '绔欑偣澶村儚(涓婁紶鏂囦欢鍚?',
  `contact` varchar(256) DEFAULT NULL COMMENT '鑱旂郴鏂瑰紡',
  `address` varchar(512) DEFAULT NULL COMMENT '鍦板潃',
  `label_price` decimal(10,2) DEFAULT NULL COMMENT '闈㈠崟浠锋牸(鍏?鍗?',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁榛樿 1鏄?0鍚?,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_express_station_name` (`name`),
  KEY `idx_ec_express_station_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢蹇€掔珯鐐?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_express_station`
--

LOCK TABLES `ec_express_station` WRITE;
/*!40000 ALTER TABLE `ec_express_station` DISABLE KEYS */;
INSERT INTO `ec_express_station` VALUES (1,'椤轰赴鏍囧揩',NULL,'95338','骞夸笢鐪佹繁鍦冲競鍗楀北鍖洪『涓版€婚儴钀ヤ笟鐐?,NULL,1,0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(2,'涓€氬揩閫?,NULL,'95311','娴欐睙鐪佹澀宸炲競浣欐澀鍖轰腑閫氳浆杩愪腑蹇?,NULL,0,0,'2026-06-24 15:38:25','2026-06-24 15:38:25');
/*!40000 ALTER TABLE `ec_express_station` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_factory`
--

DROP TABLE IF EXISTS `ec_factory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_factory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '宸ュ巶涓婚敭',
  `name` varchar(128) NOT NULL COMMENT '宸ュ巶鍚嶇О',
  `factory_type` varchar(16) NOT NULL DEFAULT 'PRODUCTION' COMMENT 'PRODUCTION/CUSTOMER',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '鑱旂郴浜?,
  `contact_phone` varchar(64) DEFAULT NULL COMMENT '鑱旂郴鏂瑰紡',
  `address` varchar(512) DEFAULT NULL COMMENT '鍦板潃',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_factory_status` (`status`),
  KEY `idx_ec_factory_type` (`factory_type`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢宸ュ巶';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_factory`
--

LOCK TABLES `ec_factory` WRITE;
/*!40000 ALTER TABLE `ec_factory` DISABLE KEYS */;
INSERT INTO `ec_factory` VALUES (1,'涓滆帪绮惧搧鏃ョ敤鍒堕€犲巶','PRODUCTION','寮犵粡鐞?,'13800138001','骞夸笢鐪佷笢鑾炲競铏庨棬闀囧伐涓氳矾 88 鍙?,'涓绘墦闄剁摲鏉€佸帹鎴垮皬浠讹紝浜ゆ湡绋冲畾','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(2,'涔変箤婧愬ご灏忓晢鍝佸伐鍘?,'PRODUCTION','鏉庢€?,'13900139002','娴欐睙鐪佷箟涔屽競鍖楄嫅琛楅亾鏄ユ櫁璺?126 鍙?,'鏀剁撼绫汇€佸鏂欏灞咃紝鏀寔璐寸墝','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(3,'瀹佹尝绔规湪瀹跺眳鐢ㄥ搧鍘?,'PRODUCTION','鐜嬪','13700137003','娴欐睙鐪佸畞娉㈠競鎱堟邯甯傞€嶆灄闀囩鑹哄洯鍖?5 鏍?,'绔圭氦缁淬€侀鍨牕鏉匡紝鍑哄彛鍝佽川','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23');
/*!40000 ALTER TABLE `ec_factory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_inbound_order`
--

DROP TABLE IF EXISTS `ec_inbound_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_inbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '杩涜揣鍗曚富閿?,
  `order_no` varchar(32) NOT NULL COMMENT '杩涜揣鍗曞彿',
  `factory_id` bigint DEFAULT NULL COMMENT '鎵€灞炲伐鍘?,
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `order_time` datetime DEFAULT NULL COMMENT '涓嬪崟鏃堕棿',
  `expected_delivery_time` datetime DEFAULT NULL COMMENT '棰勬敹璐ф椂闂?,
  `actual_receipt_time` datetime DEFAULT NULL COMMENT '瀹為檯鏀惰揣鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_inbound_order_no` (`order_no`),
  KEY `idx_ec_inbound_order_status` (`status`),
  KEY `idx_ec_inbound_order_factory` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢杩涜揣鍗?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_inbound_order`
--

LOCK TABLES `ec_inbound_order` WRITE;
/*!40000 ALTER TABLE `ec_inbound_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_inbound_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_inbound_order_line`
--

DROP TABLE IF EXISTS `ec_inbound_order_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_inbound_order_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏄庣粏涓婚敭',
  `order_id` bigint NOT NULL COMMENT '杩涜揣鍗?ID',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `quantity` int NOT NULL COMMENT '涓嬪崟鏁伴噺',
  `received_quantity` int DEFAULT NULL COMMENT '瀹為檯鏀惰揣鏁伴噺',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_inbound_order_line_order` (`order_id`),
  KEY `idx_ec_inbound_order_line_sku` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢杩涜揣鍗曟槑缁?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_inbound_order_line`
--

LOCK TABLES `ec_inbound_order_line` WRITE;
/*!40000 ALTER TABLE `ec_inbound_order_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_inbound_order_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_inventory`
--

DROP TABLE IF EXISTS `ec_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '搴撳瓨涓婚敭',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '搴撳瓨鏁伴噺',
  `ignore_alert` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁蹇界暐棰勮 1鏄?0鍚?,
  `alert_threshold` int NOT NULL DEFAULT '0' COMMENT '棰勮鏁伴噺(搴撳瓨<=璇ュ€间笖鏈拷鐣ラ璀︽椂鎶ヨ)',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_inventory_sku_code` (`sku_code`),
  KEY `idx_ec_inventory_quantity` (`quantity`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢搴撳瓨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_inventory`
--

LOCK TABLES `ec_inventory` WRITE;
/*!40000 ALTER TABLE `ec_inventory` DISABLE KEYS */;
INSERT INTO `ec_inventory` VALUES (1,'MUG-W-350',120,0,20,0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(2,'MUG-B-350',8,0,10,0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(3,'BOX-S-3L',200,0,30,0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(4,'BOX-L-8L',45,1,15,0,'2026-06-24 15:38:24','2026-06-24 15:38:24');
/*!40000 ALTER TABLE `ec_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_inventory_log`
--

DROP TABLE IF EXISTS `ec_inventory_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_inventory_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '璁板綍涓婚敭',
  `inventory_id` bigint NOT NULL COMMENT '搴撳瓨琛?ID',
  `change_type` varchar(16) NOT NULL COMMENT '鏀瑰姩鏂瑰紡 DEDUCT鎵ｉ櫎 RECLAIM鍥炴敹',
  `change_qty` int NOT NULL COMMENT '鏀瑰姩鏁伴噺(姝ｆ暟)',
  `ref_type` varchar(32) DEFAULT NULL COMMENT '鍏宠仈绫诲瀷 INBOUND_ORDER 绛?,
  `ref_id` bigint DEFAULT NULL COMMENT '鍏宠仈涓氬姟 ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_inventory_log_inventory` (`inventory_id`),
  KEY `idx_ec_inventory_log_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢搴撳瓨鎿嶄綔璁板綍';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_inventory_log`
--

LOCK TABLES `ec_inventory_log` WRITE;
/*!40000 ALTER TABLE `ec_inventory_log` DISABLE KEYS */;
INSERT INTO `ec_inventory_log` VALUES (1,1,'RECLAIM',50,NULL,NULL,NULL,0,'2026-06-24 15:38:24'),(2,1,'DEDUCT',12,NULL,NULL,NULL,0,'2026-06-24 15:38:24');
/*!40000 ALTER TABLE `ec_inventory_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_listing_link`
--

DROP TABLE IF EXISTS `ec_listing_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_listing_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '閾炬帴涓婚敭',
  `name` varchar(256) NOT NULL COMMENT '閾炬帴鍚嶇О',
  `shop_id` bigint NOT NULL COMMENT '鎵€灞炲簵閾?ID',
  `platform_url` varchar(1024) DEFAULT NULL COMMENT '骞冲彴鍟嗗搧閾炬帴URL',
  `product_id` bigint DEFAULT NULL COMMENT '鍏宠仈鍟嗗搧SPU ID',
  `listing_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '涓婃灦鏃堕棿',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_listing_link_shop` (`shop_id`),
  KEY `idx_ec_listing_link_time` (`listing_time`),
  KEY `idx_ec_listing_link_status` (`status`),
  KEY `idx_ec_listing_link_product` (`product_id`),
  KEY `idx_ec_listing_link_shop_name` (`shop_id`,`name`(128))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢涓婃灦閾炬帴';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_listing_link`
--

LOCK TABLES `ec_listing_link` WRITE;
/*!40000 ALTER TABLE `ec_listing_link` DISABLE KEYS */;
INSERT INTO `ec_listing_link` VALUES (1,'椹厠鏉弻瑙勬牸閾炬帴',2,NULL,1,'2025-06-01 10:00:00','娣樺疂涓婚摼鎺?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:26');
/*!40000 ALTER TABLE `ec_listing_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_listing_link_product`
--

DROP TABLE IF EXISTS `ec_listing_link_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_listing_link_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `link_id` bigint NOT NULL COMMENT '涓婃灦閾炬帴 ID',
  `product_id` bigint NOT NULL COMMENT '鍟嗗搧 SPU ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_listing_link_product` (`link_id`,`product_id`),
  KEY `idx_ec_listing_link_product_link` (`link_id`),
  KEY `idx_ec_listing_link_product_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='涓婃灦閾炬帴鍏宠仈鍟嗗搧';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_listing_link_product`
--

LOCK TABLES `ec_listing_link_product` WRITE;
/*!40000 ALTER TABLE `ec_listing_link_product` DISABLE KEYS */;
INSERT INTO `ec_listing_link_product` VALUES (1,1,1,0,0,'2026-06-24 15:38:26','2026-06-24 15:38:26');
/*!40000 ALTER TABLE `ec_listing_link_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_listing_link_sku`
--

DROP TABLE IF EXISTS `ec_listing_link_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_listing_link_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `link_id` bigint NOT NULL COMMENT '涓婃灦閾炬帴 ID',
  `sku_name` varchar(256) NOT NULL COMMENT '閾炬帴 SKU 灞曠ず鍚嶇О',
  `sku_codes` varchar(1024) NOT NULL COMMENT '瀵瑰簲 SKU 璐у彿锛屽涓嫳鏂囬€楀彿鍒嗛殧',
  `discount_pct` decimal(5,2) NOT NULL DEFAULT '100.00' COMMENT '鎶樻墸鎶樻暟(90=9鎶?',
  `coupon_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '浼樻儬鍒搁噾棰?鍏?',
  `min_set_amount` decimal(12,2) DEFAULT NULL COMMENT '鏈€浣庤缃噾棰?鍏?',
  `cost_price` decimal(12,2) DEFAULT NULL COMMENT '鎴愭湰浠锋牸(鍏冿紝鍚钩鍙拌垂)',
  `base_cost_amount` decimal(12,2) DEFAULT NULL COMMENT '鍩虹鎴愭湰=SKU+绾哥+蹇€?,
  `platform_fee_amount` decimal(12,2) DEFAULT NULL COMMENT '骞冲彴璐?鐩堜簭骞宠　鍙ｅ緞)',
  `actual_set_amount` decimal(12,2) DEFAULT NULL COMMENT '鐪熷疄璁剧疆閲戦(鍏冿紝鍙墜鍔ㄥ～鍐?',
  `profit` decimal(12,2) DEFAULT NULL COMMENT '鍒╂鼎(鍏?',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_listing_link_sku_link` (`link_id`),
  KEY `idx_ec_listing_link_sku_link_name` (`link_id`,`sku_name`(128))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='涓婃灦閾炬帴 SKU 淇℃伅';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_listing_link_sku`
--

LOCK TABLES `ec_listing_link_sku` WRITE;
/*!40000 ALTER TABLE `ec_listing_link_sku` DISABLE KEYS */;
INSERT INTO `ec_listing_link_sku` VALUES (1,1,'鐧借壊 350ml','MUG-W-350',90.00,2.00,59.98,52.18,47.40,4.78,59.90,-0.06,1,0,'2026-06-24 15:38:26','2026-06-24 15:38:26'),(2,1,'榛戣壊 350ml','MUG-B-350',90.00,2.00,62.40,54.36,49.40,4.96,62.90,0.42,2,0,'2026-06-24 15:38:26','2026-06-24 15:38:26');
/*!40000 ALTER TABLE `ec_listing_link_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_outbound_order`
--

DROP TABLE IF EXISTS `ec_outbound_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_outbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鍑鸿揣鍗曚富閿?,
  `order_no` varchar(32) NOT NULL COMMENT '鍑鸿揣鍗曞彿',
  `factory_id` bigint DEFAULT NULL COMMENT '鎵€灞炲伐鍘?,
  `customer_factory_id` bigint DEFAULT NULL COMMENT '瀹㈡埛宸ュ巶 ID',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `order_time` datetime DEFAULT NULL COMMENT '鍒涘崟鏃堕棿',
  `expected_ship_time` datetime DEFAULT NULL COMMENT '棰勫嚭璐ф椂闂?,
  `actual_ship_time` datetime DEFAULT NULL COMMENT '瀹為檯鍑鸿揣鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_outbound_order_no` (`order_no`),
  KEY `idx_ec_outbound_order_status` (`status`),
  KEY `idx_ec_outbound_order_factory` (`factory_id`),
  KEY `idx_ec_outbound_order_customer` (`customer_factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鍑鸿揣鍗?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_outbound_order`
--

LOCK TABLES `ec_outbound_order` WRITE;
/*!40000 ALTER TABLE `ec_outbound_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_outbound_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_outbound_order_line`
--

DROP TABLE IF EXISTS `ec_outbound_order_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_outbound_order_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏄庣粏涓婚敭',
  `order_id` bigint NOT NULL COMMENT '鍑鸿揣鍗?ID',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `quantity` int NOT NULL COMMENT '璁″垝鍑鸿揣鏁伴噺',
  `shipped_quantity` int DEFAULT NULL COMMENT '瀹為檯鍑鸿揣鏁伴噺',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_outbound_order_line_order` (`order_id`),
  KEY `idx_ec_outbound_order_line_sku` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鍑鸿揣鍗曟槑缁?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_outbound_order_line`
--

LOCK TABLES `ec_outbound_order_line` WRITE;
/*!40000 ALTER TABLE `ec_outbound_order_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_outbound_order_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_platform`
--

DROP TABLE IF EXISTS `ec_platform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '骞冲彴涓婚敭',
  `name` varchar(128) NOT NULL COMMENT '骞冲彴鍚嶇О',
  `name_en` varchar(128) DEFAULT NULL COMMENT '骞冲彴鑻辨枃鍚嶇О',
  `avatar_url` varchar(256) DEFAULT NULL COMMENT '骞冲彴澶村儚(涓婁紶鏂囦欢鍚?',
  `platform_code` int NOT NULL COMMENT '骞冲彴鏍囪瘑(鏋氫妇 int)',
  `channel_type` varchar(16) NOT NULL DEFAULT 'ONLINE' COMMENT '娓犻亾妯″紡 ONLINE/OFFLINE',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_platform_code` (`platform_code`),
  KEY `idx_ec_platform_channel` (`channel_type`),
  KEY `idx_ec_platform_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢骞冲彴';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_platform`
--

LOCK TABLES `ec_platform` WRITE;
/*!40000 ALTER TABLE `ec_platform` DISABLE KEYS */;
INSERT INTO `ec_platform` VALUES (1,'1688','1688',NULL,1,'ONLINE','闃块噷宸村反 1688 鎵瑰彂/閲囪喘','ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(2,'娣樺疂','Taobao',NULL,2,'ONLINE','娣樺疂 C 搴?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(3,'澶╃尗','Tmall',NULL,3,'ONLINE','澶╃尗 B 搴?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(4,'鎷煎澶?,'Pinduoduo',NULL,4,'ONLINE','鎷煎澶?POP 搴?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(5,'鎶栧簵','Douyin Shop',NULL,5,'ONLINE','鎶栭煶鐢靛晢/鎶栧簵','ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(6,'浜笢','JD',NULL,6,'ONLINE','浜笢 POP 搴?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(7,'绾夸笅闂ㄥ簵','Offline Store',NULL,0,'OFFLINE','鐩磋惀/鍔犵洘绾夸笅闂ㄥ簵','ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(8,'绾夸笅鎵瑰彂','Offline Wholesale',NULL,0,'OFFLINE','妗ｅ彛/灞曚細/绾夸笅鎵瑰彂','ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25');
/*!40000 ALTER TABLE `ec_platform` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_product`
--

DROP TABLE IF EXISTS `ec_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SPU 涓婚敭',
  `factory_id` bigint DEFAULT NULL COMMENT '鎵€灞炲伐鍘?,
  `name` varchar(256) NOT NULL COMMENT '鍟嗗搧鍚嶇О(SPU)',
  `description` text COMMENT '鍟嗗搧鎻忚堪',
  `rebate_pct` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '閫€鐐?鐧惧垎姣旓紝濡?5.50 琛ㄧず 5.5%)',
  `image_name` varchar(256) DEFAULT NULL COMMENT '鍥剧墖鏂囦欢鍚?,
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_product_status` (`status`),
  KEY `idx_ec_product_factory` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鍟嗗搧 SPU';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_product`
--

LOCK TABLES `ec_product` WRITE;
/*!40000 ALTER TABLE `ec_product` DISABLE KEYS */;
INSERT INTO `ec_product` VALUES (1,1,'绠€绾﹂櫠鐡烽┈鍏嬫澂濂楄','楂樻俯闄剁摲锛屽彲杩涙礂纰楁満锛岀ぜ鐩掕',5.50,'mug-set-main.jpg','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(2,2,'澶氬眰鍘ㄦ埧鏀剁撼鐩?,'PP 鏉愯川锛屽彲鍙犲姞锛屽帹鎴?琛ｆ煖閫氱敤',8.00,'storage-box-main.jpg','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(3,3,'绔圭氦缁撮鍨洓浠跺','澶╃劧绔圭氦缁达紝闃叉粦鑰愮儹锛屽洓鑹蹭竴缁?,6.00,'bamboo-mat-main.jpg','ENABLED',0,'2026-06-24 15:38:23','2026-06-24 15:38:23');
/*!40000 ALTER TABLE `ec_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_purchase_order_config`
--

DROP TABLE IF EXISTS `ec_purchase_order_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_purchase_order_config` (
  `id` bigint NOT NULL COMMENT '鍥哄畾涓?1',
  `title` varchar(128) NOT NULL DEFAULT '鍞崄鍢夐噰璐崟' COMMENT '閲囪喘鍗曟爣棰?,
  `address` varchar(512) DEFAULT NULL COMMENT '鍦板潃',
  `tel` varchar(64) DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
  `requirement_items` text COMMENT '璁㈠崟瑕佹眰 JSON 鏁扮粍',
  `note_items` text COMMENT '娉ㄦ剰浜嬮」 JSON 鏁扮粍',
  `prepared_by` varchar(64) DEFAULT NULL COMMENT '鍒跺崟浜虹鍚?,
  `prepared_phone` varchar(64) DEFAULT NULL COMMENT '鍒跺崟浜虹數璇?,
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '鏀惰揣浜?,
  `receiver_phone` varchar(64) DEFAULT NULL COMMENT '鏀惰揣鐢佃瘽',
  `receiver_address` varchar(512) DEFAULT NULL COMMENT '鏀惰揣鍦板潃',
  `company_no` varchar(64) DEFAULT NULL COMMENT '鍏徃缂栧彿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢閲囪喘鍗曠郴缁熼厤缃?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_purchase_order_config`
--

LOCK TABLES `ec_purchase_order_config` WRITE;
/*!40000 ALTER TABLE `ec_purchase_order_config` DISABLE KEYS */;
INSERT INTO `ec_purchase_order_config` VALUES (1,'鍞崄鍢夐噰璐崟','鍦板潃锛氭睍澶村競婢勬捣鍖鸿幉涓嬮晣涓滄咕鏂囧寲鍏洯','TEL锛?8819446360','[\"鍑鸿揣鏃堕棿銆佹棩鏈熶笉鑳藉啀鎷栧悗锛佷骇鍝佸繀椤婚€氳繃缇庡浗绔欏叏妫€娴嬩互鍙奀PSIA锛孋PC妫€娴嬨€俓",\"澶ц揣浜у搧棰滆壊蹇呴』璺熷乏涓婅鍥剧墖涓€鏍凤紝涓嶈兘鏇存敼銆俓",\"鎸夋柊绾哥瑙勬牸鍖呰锛岀‘淇濊揣鐗╃ǔ鍥猴紝杩愯緭杩囩▼涓笉浼氱牬鎹熴€俓"]','[\"鏈崟涓洪噰璐悎鍚岋紝璇峰伐鍘傜瀛楃洊绔犲洖浼狅紝骞跺Ε鍠勪繚绠°€俓",\"浜у搧鍚嶇О蹇呴』涓庨噰璐崟涓€鑷达紝濡傛湁鍙樻洿璇锋彁鍓嶆矡閫氱‘璁ゃ€俓",\"璇锋寜绾﹀畾鏃堕棿浜よ揣锛屽鏈夊欢璇绗竴鏃堕棿閫氱煡鎴戝徃銆俓",\"璐ф涓庡彂璐т簨瀹滆涓庨噰璐仈绯讳汉鏍稿鍚庢墽琛屻€俓"]','寮犲皬濮?,'18819446360','寮犲皬濮?,'18819446360','瑙佷笂闈㈠湴鍧€','','2026-06-27 21:59:39');
/*!40000 ALTER TABLE `ec_purchase_order_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_sales_order`
--

DROP TABLE IF EXISTS `ec_sales_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_sales_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '閿€鍞鍗曚富閿?,
  `order_no` varchar(32) NOT NULL COMMENT '绯荤粺璁㈠崟鍙?SOyyyyMMddxxxx',
  `shop_id` bigint NOT NULL COMMENT '鎵€灞炲簵閾?ID',
  `platform_order_no` varchar(64) DEFAULT NULL COMMENT '骞冲彴璁㈠崟鍙凤紙瀵煎叆鍘婚噸閿級',
  `source` varchar(16) NOT NULL DEFAULT 'MANUAL' COMMENT '鏉ユ簮 MANUAL/IMPORT/API',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PAID/PARTIAL_SHIPPED/SHIPPED/PARTIAL_REFUND/COMPLETED/CANCELLED/REFUNDED',
  `platform_status` varchar(64) DEFAULT NULL COMMENT '骞冲彴鍘熷鐘舵€佹枃妗?,
  `express_station_id` bigint DEFAULT NULL COMMENT '蹇€掔珯鐐?ID锛屽叧鑱?ec_express_station.id锛岀敤浜庤绠楃湡瀹炶繍璐?,
  `order_time` datetime NOT NULL COMMENT '涓嬪崟鏃堕棿',
  `pay_time` datetime DEFAULT NULL COMMENT '鏀粯鏃堕棿',
  `ship_time` datetime DEFAULT NULL COMMENT '棣栨潯鏄庣粏鍙戣揣鏃堕棿',
  `complete_time` datetime DEFAULT NULL COMMENT '璁㈠崟瀹屾垚鏃堕棿',
  `buyer_name` varchar(128) DEFAULT NULL COMMENT '涔板鏄电О/濮撳悕',
  `buyer_phone` varchar(32) DEFAULT NULL COMMENT '涔板鐢佃瘽',
  `receive_province` varchar(64) DEFAULT NULL COMMENT '鏀惰揣鐪?鐢?receive_address 鑷姩瑙ｆ瀽)',
  `receive_city` varchar(64) DEFAULT NULL COMMENT '鏀惰揣甯?,
  `receive_district` varchar(64) DEFAULT NULL COMMENT '鏀惰揣鍖?,
  `receive_address` varchar(512) DEFAULT NULL COMMENT '璇︾粏鍦板潃',
  `tracking_number` varchar(64) DEFAULT NULL COMMENT '蹇€掑崟鍙?,
  `buyer_remark` varchar(512) DEFAULT NULL COMMENT '涔板鐣欒█',
  `seller_remark` varchar(512) DEFAULT NULL COMMENT '鍗栧澶囨敞',
  `received_amount` decimal(12,2) DEFAULT NULL COMMENT '璁㈠崟瀹炴敹閲戦(鍏?',
  `total_cost_amount` decimal(12,2) DEFAULT NULL COMMENT '璁㈠崟鎬绘垚鏈?鍏冿紝鏄庣粏姹囨€诲揩鐓?',
  `freight_amount` decimal(12,2) DEFAULT NULL COMMENT '涔板浠樿繍璐?鍏?',
  `estimated_freight_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '璇曠畻杩愯垂(鍏冿紝鎸夌珯鐐?鐪?閲嶉噺)',
  `actual_freight_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '鐪熷疄杩愯垂(鍏冿紝鏈堢粨蹇€掕处鍗曟寜杩愬崟鍙峰洖濉?',
  `order_coupon_amount` decimal(12,2) DEFAULT NULL COMMENT '璁㈠崟绾т紭鎯犲埜(鍏?',
  `platform_fee_amount` decimal(12,2) DEFAULT NULL COMMENT '骞冲彴璐瑰悎璁″揩鐓?鍏?',
  `profit_amount` decimal(12,2) DEFAULT NULL COMMENT '鍒╂鼎鍚堣蹇収(鍏冿紝鍚€€娆句簭鎹熷悗)',
  `total_loss_amount` decimal(12,2) DEFAULT NULL COMMENT '閫€娆句簭鎹熷悎璁?鍏?',
  `has_shortage` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁瀛樺湪娆犺揣 1鏄?0鍚?,
  `import_batch_id` bigint DEFAULT NULL COMMENT '鍏宠仈瀵煎叆鎵规 ID',
  `platform_raw_json` json DEFAULT NULL COMMENT '骞冲彴/瀵煎叆鍘熷 JSON 蹇収',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_sales_order_no` (`order_no`),
  UNIQUE KEY `uk_ec_sales_order_platform` (`shop_id`,`platform_order_no`),
  KEY `idx_ec_sales_order_shop` (`shop_id`),
  KEY `idx_ec_sales_order_status` (`status`),
  KEY `idx_ec_sales_order_order_time` (`order_time`),
  KEY `idx_ec_sales_order_express_station` (`express_station_id`),
  KEY `idx_ec_sales_order_tracking` (`tracking_number`),
  KEY `idx_ec_sales_order_import_batch` (`import_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢閿€鍞鍗?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_sales_order`
--

LOCK TABLES `ec_sales_order` WRITE;
/*!40000 ALTER TABLE `ec_sales_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_sales_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_sales_order_inventory_deduct`
--

DROP TABLE IF EXISTS `ec_sales_order_inventory_deduct`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_sales_order_inventory_deduct` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `order_id` bigint NOT NULL COMMENT '閿€鍞鍗?ID',
  `order_line_id` bigint NOT NULL COMMENT '璁㈠崟鏄庣粏 ID',
  `shortage_id` bigint DEFAULT NULL COMMENT '鍏宠仈娆犺揣璁板綍 ID',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `inventory_id` bigint DEFAULT NULL COMMENT 'ec_inventory.id',
  `inventory_log_id` bigint DEFAULT NULL COMMENT 'ec_inventory_log.id',
  `deduct_qty` int NOT NULL COMMENT '鏈鎵ｉ櫎鏁伴噺',
  `before_qty` int DEFAULT NULL COMMENT '鎵ｅ墠搴撳瓨',
  `after_qty` int DEFAULT NULL COMMENT '鎵ｅ悗搴撳瓨',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_sales_order_deduct_order` (`order_id`),
  KEY `idx_ec_sales_order_deduct_line` (`order_line_id`),
  KEY `idx_ec_sales_order_deduct_sku` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='閿€鍞鍗曞簱瀛樻墸鍑忚褰?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_sales_order_inventory_deduct`
--

LOCK TABLES `ec_sales_order_inventory_deduct` WRITE;
/*!40000 ALTER TABLE `ec_sales_order_inventory_deduct` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_sales_order_inventory_deduct` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_sales_order_line`
--

DROP TABLE IF EXISTS `ec_sales_order_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_sales_order_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏄庣粏涓婚敭',
  `order_id` bigint NOT NULL COMMENT '閿€鍞鍗?ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `listing_link_sku_id` bigint DEFAULT NULL COMMENT '涓婃灦閾炬帴 SKU ID锛堝尮閰?ec_listing_link_sku.id锛?,
  `link_name` varchar(256) DEFAULT NULL COMMENT '閾炬帴鍚嶇О蹇収 ec_listing_link.name',
  `sku_spec_name` varchar(256) DEFAULT NULL COMMENT 'SKU灞曠ず鍚嶇О蹇収 ec_listing_link_sku.sku_name',
  `sku_codes` varchar(1024) DEFAULT NULL COMMENT '璐у彿蹇収锛岄€楀彿鍒嗛殧',
  `sku_quantity` int NOT NULL DEFAULT '1' COMMENT 'SKU鏁伴噺(閾炬帴SKU濂楁暟)',
  `shipped_quantity` int NOT NULL DEFAULT '0' COMMENT '宸插彂璐у鏁?,
  `short_quantity` int NOT NULL DEFAULT '0' COMMENT '娆犺揣濂楁暟',
  `status` varchar(16) NOT NULL DEFAULT 'PAID' COMMENT 'PAID/SHIPPED/COMPLETED/CANCELLED/PARTIAL_REFUND/REFUNDED/RETURNED',
  `platform_line_status` varchar(64) DEFAULT NULL COMMENT '骞冲彴瀛愯鍗曞師濮嬬姸鎬?,
  `refund_type` varchar(16) DEFAULT NULL COMMENT '閫€娆剧被鍨?NONE/REFUND_ONLY宸插彂璐ч€€娆?RETURN_REFUND閫€璐ч€€娆?,
  `refund_time` datetime DEFAULT NULL COMMENT '閫€娆?閫€璐ф椂闂?,
  `refund_amount` decimal(12,2) DEFAULT NULL COMMENT '閫€娆鹃噾棰?鍏?',
  `loss_amount` decimal(12,2) DEFAULT NULL COMMENT '浜忔崯閲戦(鍏?锛屽凡鍙戣揣閫€娆?閫€璐т笉閫€搴撳瓨锛屾寜鎴愭湰璁颁簭',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT '鎴愪氦鍗曚环(鍏?濂?',
  `discount_pct` decimal(5,2) DEFAULT NULL COMMENT '鎶樻墸鎶樻暟蹇収(90=9鎶?',
  `line_coupon_amount` decimal(12,2) DEFAULT NULL COMMENT '琛岀骇浼樻儬鍒?鍏?',
  `line_received_amount` decimal(12,2) DEFAULT NULL COMMENT '琛屽疄鏀堕噾棰?鍏?',
  `sku_amount` decimal(12,2) DEFAULT NULL COMMENT 'SKU鍞环鍚堣蹇収',
  `carton_amount` decimal(12,2) DEFAULT NULL COMMENT '绾哥鎴愭湰蹇収',
  `express_amount` decimal(12,2) DEFAULT NULL COMMENT '璇曠畻蹇€掓垚鏈揩鐓?,
  `base_cost_amount` decimal(12,2) DEFAULT NULL COMMENT '鍩虹鎴愭湰蹇収',
  `platform_fee_amount` decimal(12,2) DEFAULT NULL COMMENT '骞冲彴璐瑰揩鐓?,
  `cost_price` decimal(12,2) DEFAULT NULL COMMENT '琛屾€绘垚鏈揩鐓?鍚钩鍙拌垂鐩堜簭骞宠　鍙ｅ緞)',
  `min_set_amount` decimal(12,2) DEFAULT NULL COMMENT '鏈€浣庤缃噾棰濆揩鐓?,
  `profit` decimal(12,2) DEFAULT NULL COMMENT '琛屽埄娑﹀揩鐓?,
  `pricing_risk` varchar(16) DEFAULT NULL COMMENT 'OK/BELOW_MIN/NEGATIVE_PROFIT',
  `platform_line_no` varchar(64) DEFAULT NULL COMMENT '骞冲彴瀛愯鍗曞彿',
  `platform_item_name` varchar(512) DEFAULT NULL COMMENT '骞冲彴鍟嗗搧鏍囬蹇収',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_sales_order_line_order` (`order_id`),
  KEY `idx_ec_sales_order_line_link_sku` (`listing_link_sku_id`),
  KEY `idx_ec_sales_order_line_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢閿€鍞鍗曟槑缁?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_sales_order_line`
--

LOCK TABLES `ec_sales_order_line` WRITE;
/*!40000 ALTER TABLE `ec_sales_order_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_sales_order_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_sales_order_shortage`
--

DROP TABLE IF EXISTS `ec_sales_order_shortage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_sales_order_shortage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '娆犺揣涓婚敭',
  `order_id` bigint NOT NULL COMMENT '閿€鍞鍗?ID',
  `order_line_id` bigint NOT NULL COMMENT '璁㈠崟鏄庣粏 ID',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `need_qty` int NOT NULL COMMENT '搴旀墸鏁伴噺',
  `deducted_qty` int NOT NULL DEFAULT '0' COMMENT '瀹炴墸鏁伴噺',
  `short_qty` int NOT NULL DEFAULT '0' COMMENT '娆犺揣鏁伴噺',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLEARED',
  `cleared_qty` int NOT NULL DEFAULT '0' COMMENT '宸叉牳閿€鏁伴噺',
  `cleared_ref_type` varchar(32) DEFAULT NULL COMMENT '鏍搁攢鏉ユ簮',
  `cleared_ref_id` bigint DEFAULT NULL COMMENT '鏍搁攢涓氬姟 ID',
  `cleared_time` datetime DEFAULT NULL COMMENT '鏍搁攢鏃堕棿',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_sales_order_shortage_order` (`order_id`),
  KEY `idx_ec_sales_order_shortage_line` (`order_line_id`),
  KEY `idx_ec_sales_order_shortage_sku` (`sku_code`),
  KEY `idx_ec_sales_order_shortage_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='閿€鍞鍗曞彂璐ф瑺璐?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_sales_order_shortage`
--

LOCK TABLES `ec_sales_order_shortage` WRITE;
/*!40000 ALTER TABLE `ec_sales_order_shortage` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_sales_order_shortage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_settlement_buyer_exclude`
--

DROP TABLE IF EXISTS `ec_settlement_buyer_exclude`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_settlement_buyer_exclude` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `shop_id` bigint DEFAULT NULL COMMENT '搴楅摵 ID锛岀┖=鍏ㄩ儴搴楅摵',
  `buyer_name` varchar(128) NOT NULL COMMENT '涔板鏄电О锛堢簿纭尮閰?trim锛?,
  `remark` varchar(256) DEFAULT NULL COMMENT '澶囨敞',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1鍚敤 0鍋滅敤',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_settlement_buyer_exclude_shop` (`shop_id`),
  KEY `idx_settlement_buyer_exclude_name` (`buyer_name`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏈堢粨缁熻-涔板鎺掗櫎';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_settlement_buyer_exclude`
--

LOCK TABLES `ec_settlement_buyer_exclude` WRITE;
/*!40000 ALTER TABLE `ec_settlement_buyer_exclude` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_settlement_buyer_exclude` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_settlement_express_bill`
--

DROP TABLE IF EXISTS `ec_settlement_express_bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_settlement_express_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `bill_month` char(7) NOT NULL COMMENT '璐﹀崟鏈堜唤 YYYY-MM',
  `express_station_id` bigint DEFAULT NULL COMMENT '蹇€掔珯鐐?ID',
  `include_label_price` tinyint NOT NULL DEFAULT '0' COMMENT '1=杩愯垂鍙犲姞闈㈠崟浠锋牸',
  `other_express` tinyint NOT NULL DEFAULT '0' COMMENT '1=鍏朵粬蹇€掑叕鍙革紙鏈尮閰嶇郴缁熺珯鐐癸級',
  `file_name` varchar(256) DEFAULT NULL COMMENT '涓婁紶鏂囦欢鍚?,
  `total_rows` int NOT NULL DEFAULT '0',
  `matched_rows` int NOT NULL DEFAULT '0',
  `unmatched_rows` int NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED/FAILED',
  `error_message` varchar(512) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `column_mapping` text COMMENT '鍒楁槧灏?JSON 蹇収',
  `header_row` int NOT NULL DEFAULT '1' COMMENT '琛ㄥご琛屽彿(1-based)',
  `data_start_row` int NOT NULL DEFAULT '2' COMMENT '鏁版嵁璧峰琛?1-based)',
  `import_mode` varchar(16) NOT NULL DEFAULT 'FILE' COMMENT 'FILE/MANUAL/MIXED',
  `gap_order_rows` int NOT NULL DEFAULT '0' COMMENT '鏈尮閰嶅彂璐?瀹屾垚璁㈠崟鏁?,
  `manual_applied_rows` int NOT NULL DEFAULT '0' COMMENT '鎵嬪姩琛ュ綍骞跺簲鐢ㄦ潯鏁?,
  PRIMARY KEY (`id`),
  KEY `idx_settlement_express_bill_month` (`bill_month`),
  KEY `idx_settlement_express_bill_station` (`express_station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏈堢粨蹇€掕处鍗曞鍏ユ壒娆?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_settlement_express_bill`
--

LOCK TABLES `ec_settlement_express_bill` WRITE;
/*!40000 ALTER TABLE `ec_settlement_express_bill` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_settlement_express_bill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_settlement_express_bill_line`
--

DROP TABLE IF EXISTS `ec_settlement_express_bill_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_settlement_express_bill_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `bill_id` bigint NOT NULL COMMENT '鎵规 ID',
  `express_station_id` bigint DEFAULT NULL COMMENT '蹇€掑叕鍙?ID',
  `source` varchar(16) NOT NULL COMMENT 'FILE/GAP_ORDER/MANUAL',
  `order_id` bigint DEFAULT NULL COMMENT '鍖归厤璁㈠崟 ID',
  `platform_order_no` varchar(64) DEFAULT NULL COMMENT '骞冲彴璁㈠崟鍙?,
  `order_no` varchar(64) DEFAULT NULL COMMENT '绯荤粺璁㈠崟鍙?,
  `tracking_number` varchar(128) DEFAULT NULL COMMENT '杩愬崟鍙?,
  `freight_amount` decimal(12,2) DEFAULT NULL COMMENT '杩愯垂',
  `settlement_destination` varchar(128) DEFAULT NULL COMMENT '缁撶畻鐩殑鍦?,
  `weight` decimal(10,3) DEFAULT NULL COMMENT '閲嶉噺(kg)',
  `ship_time` datetime DEFAULT NULL COMMENT '鍙戣揣鏃堕棿',
  `match_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'MATCHED/UNMATCHED/PENDING/APPLIED',
  `remark` varchar(256) DEFAULT NULL COMMENT '澶囨敞',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_express_bill_line_bill` (`bill_id`),
  KEY `idx_express_bill_line_order` (`order_id`),
  KEY `idx_express_bill_line_tracking` (`tracking_number`(32)),
  KEY `idx_express_bill_line_station` (`express_station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏈堢粨蹇€掕处鍗曟槑缁?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_settlement_express_bill_line`
--

LOCK TABLES `ec_settlement_express_bill_line` WRITE;
/*!40000 ALTER TABLE `ec_settlement_express_bill_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_settlement_express_bill_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_settlement_order_decision`
--

DROP TABLE IF EXISTS `ec_settlement_order_decision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_settlement_order_decision` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `shop_id` bigint NOT NULL COMMENT '搴楅摵 ID',
  `order_id` bigint NOT NULL COMMENT '閿€鍞鍗?ID',
  `settlement_month` char(7) NOT NULL COMMENT '缁熻鏈堜唤 YYYY-MM',
  `included` tinyint NOT NULL DEFAULT '0' COMMENT '1绾冲叆 0涓嶇撼鍏?,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_order_month` (`order_id`,`settlement_month`),
  KEY `idx_settlement_decision_shop_month` (`shop_id`,`settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏈堢粨缁熻-璁㈠崟绾冲叆鍐崇瓥';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_settlement_order_decision`
--

LOCK TABLES `ec_settlement_order_decision` WRITE;
/*!40000 ALTER TABLE `ec_settlement_order_decision` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_settlement_order_decision` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_settlement_snapshot`
--

DROP TABLE IF EXISTS `ec_settlement_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_settlement_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `settlement_month` char(7) NOT NULL COMMENT '缁熻鏈堜唤 YYYY-MM',
  `express_bill_imported` tinyint NOT NULL DEFAULT '0' COMMENT '缁熻鏃舵槸鍚﹀凡瀵煎叆蹇€掕处鍗?,
  `snapshot_json` longtext NOT NULL COMMENT 'EcMonthlySettlementVO JSON 蹇収',
  `calculated_at` datetime NOT NULL COMMENT '缁熻瀹屾垚鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_snapshot_month` (`settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏈堢粨缁熻缁撴灉蹇収';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_settlement_snapshot`
--

LOCK TABLES `ec_settlement_snapshot` WRITE;
/*!40000 ALTER TABLE `ec_settlement_snapshot` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_settlement_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_shop`
--

DROP TABLE IF EXISTS `ec_shop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '搴楅摵涓婚敭',
  `name` varchar(128) NOT NULL COMMENT '搴楅摵鍚嶇О',
  `name_en` varchar(128) DEFAULT NULL COMMENT '搴楅摵鑻辨枃鍚嶇О',
  `avatar_url` varchar(256) DEFAULT NULL COMMENT '搴楅摵澶村儚(涓婁紶鏂囦欢鍚?',
  `platform_id` bigint NOT NULL COMMENT '鎵€灞炲钩鍙?ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `category_commission_pct` decimal(5,2) DEFAULT NULL COMMENT '绫荤洰/浜ゆ槗浣ｉ噾%',
  `tech_service_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '鍩虹鎶€鏈湇鍔¤垂%',
  `payment_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '鏀粯鎵嬬画璐?',
  `promotion_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '鎺ㄥ箍/骞垮憡榛樿鎵ｇ偣%',
  `fulfillment_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '灞ョ害/浠ｅ彂鏈嶅姟璐?',
  `return_service_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '閫€璐?閫嗗悜鐗╂祦鏈嶅姟璐圭巼%',
  `installment_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '鍒嗘湡/鑺卞憲鎵嬬画璐?',
  `activity_service_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '娲诲姩/澶т績鎶€鏈湇鍔¤垂%',
  `annual_platform_fee` decimal(12,2) DEFAULT NULL COMMENT '骞冲彴骞磋垂/杞欢鏈嶅姟璐?鍏?骞?',
  `deposit_amount` decimal(12,2) DEFAULT NULL COMMENT '淇濊瘉閲?鍏?',
  `shipping_insurance_fee` decimal(10,2) DEFAULT NULL COMMENT '榛樿鍗曠瑪杩愯垂闄?鍏?',
  `other_fee_pct` decimal(5,2) DEFAULT NULL COMMENT '鍏朵粬缁煎悎鎵ｇ偣%',
  `other_fee_remark` varchar(256) DEFAULT NULL COMMENT '鍏朵粬璐圭敤璇存槑',
  `default_receive_province` varchar(64) DEFAULT '骞夸笢鐪? COMMENT '榛樿鏀惰揣鐪佷唤(蹇€掕瘯绠?',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_shop_platform` (`platform_id`),
  KEY `idx_ec_shop_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢搴楅摵';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_shop`
--

LOCK TABLES `ec_shop` WRITE;
/*!40000 ALTER TABLE `ec_shop` DISABLE KEYS */;
INSERT INTO `ec_shop` VALUES (1,'1688 婧愬ご宸ュ巶搴?,'1688 Source Factory',NULL,1,'涓讳緵鎵瑰彂琛ヨ揣',0.00,0.60,0.60,0.00,NULL,NULL,NULL,NULL,6688.00,3000.00,NULL,NULL,'璇氫俊閫氬勾璐规寜妗ｏ紝姝ゅ涓虹ず渚?,'骞夸笢鐪?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(2,'娣樺疂 C 搴?鏃ョ敤瀹跺眳','Taobao Home Store',NULL,2,'C 搴椾富搴?,2.00,0.60,0.60,5.00,NULL,NULL,NULL,NULL,NULL,NULL,0.50,NULL,NULL,'骞夸笢鐪?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(3,'鎷煎澶氭棗鑸板簵','PDD Flagship',NULL,4,'鐧句嚎琛ヨ创娲诲姩搴?,0.60,0.60,0.60,NULL,NULL,NULL,NULL,1.00,NULL,NULL,0.30,NULL,NULL,'骞夸笢鐪?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(4,'鎶栧簵瀹樻柟搴?,'Douyin Official',NULL,5,'鐩存挱+鍟嗗搧鍗?,3.00,0.00,0.60,8.00,NULL,NULL,NULL,NULL,NULL,5000.00,NULL,NULL,NULL,'骞夸笢鐪?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25'),(5,'涓滆帪灞曞巺鐩磋惀搴?,'Dongguan Showroom',NULL,7,'绾夸笅闆跺敭',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0.00,'绾夸笅鏃犲钩鍙颁剑閲戯紝姣涘埄鍦ㄥ晢鍝佸畾浠蜂腑浣撶幇','骞夸笢鐪?,'ENABLED',0,'2026-06-24 15:38:25','2026-06-24 15:38:25');
/*!40000 ALTER TABLE `ec_shop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_sku`
--

DROP TABLE IF EXISTS `ec_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SKU 涓婚敭',
  `product_id` bigint NOT NULL COMMENT '鎵€灞?SPU',
  `sku_code` varchar(64) NOT NULL COMMENT '璐у彿',
  `spec_name` varchar(128) DEFAULT NULL COMMENT '瑙勬牸鍚嶇О',
  `rebate_pct` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '閫€鐐?鐧惧垎姣旓紝璁＄畻浠?SKU 涓哄噯)',
  `image_name` varchar(256) DEFAULT NULL COMMENT '鍥剧墖鏂囦欢鍚?,
  `carton_id` bigint DEFAULT NULL COMMENT '鍖归厤绾哥',
  `sale_price` decimal(12,2) DEFAULT NULL COMMENT '閿€鍞环',
  `product_length_cm` decimal(10,2) DEFAULT NULL COMMENT '鍗曞搧闀?cm)',
  `product_width_cm` decimal(10,2) DEFAULT NULL COMMENT '鍗曞搧瀹?cm)',
  `product_height_cm` decimal(10,2) DEFAULT NULL COMMENT '鍗曞搧楂?cm)',
  `carton_length_cm` decimal(10,2) DEFAULT NULL COMMENT '澶栫闀?cm)',
  `carton_width_cm` decimal(10,2) DEFAULT NULL COMMENT '澶栫瀹?cm)',
  `carton_height_cm` decimal(10,2) DEFAULT NULL COMMENT '澶栫楂?cm)',
  `carton_gross_weight_kg` decimal(10,3) DEFAULT NULL COMMENT '澶栫姣涢噸(kg)',
  `carton_net_weight_kg` decimal(10,3) DEFAULT NULL COMMENT '澶栫鍑€閲?kg)',
  `units_per_carton` int NOT NULL DEFAULT '1' COMMENT '澶栫瑁呬骇鍝佹暟閲?,
  `status` varchar(16) NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/OFF_SALE/DRAFT',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_sku_code` (`sku_code`),
  KEY `idx_ec_sku_product` (`product_id`),
  KEY `idx_ec_sku_carton` (`carton_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鍟嗗搧 SKU';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_sku`
--

LOCK TABLES `ec_sku` WRITE;
/*!40000 ALTER TABLE `ec_sku` DISABLE KEYS */;
INSERT INTO `ec_sku` VALUES (1,1,'MUG-W-350','鐧借壊 350ml',5.50,'mug-white-350.jpg',1,29.90,9.50,9.50,12.00,42.00,32.00,28.00,8.500,7.800,24,'ON_SALE',0,'2026-06-24 15:38:23','2026-06-24 15:38:23'),(2,1,'MUG-B-350','榛戣壊 350ml',6.00,'mug-black-350.jpg',1,31.90,9.50,9.50,12.00,42.00,32.00,28.00,8.600,7.900,24,'ON_SALE',0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(3,2,'BOX-S-3L','灏忓彿 3L',8.00,'box-small-3l.jpg',2,18.50,20.00,15.00,12.00,62.00,42.00,38.00,6.200,5.600,20,'ON_SALE',0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(4,2,'BOX-L-8L','澶у彿 8L',8.00,'box-large-8l.jpg',2,32.00,32.00,22.00,18.00,68.00,48.00,42.00,9.800,8.900,12,'ON_SALE',0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(5,3,'MAT-4PC-GR','鍥涗欢濂?鐏拌壊',6.00,'mat-4pc-gray.jpg',3,45.00,30.00,45.00,0.40,48.00,34.00,26.00,5.500,5.000,30,'ON_SALE',0,'2026-06-24 15:38:24','2026-06-24 15:38:24'),(6,3,'MAT-4PC-BE','鍥涗欢濂?绫宠壊',6.00,'mat-4pc-beige.jpg',3,45.00,30.00,45.00,0.40,48.00,34.00,26.00,5.500,5.000,30,'ON_SALE',0,'2026-06-24 15:38:24','2026-06-24 15:38:24');
/*!40000 ALTER TABLE `ec_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_stocktake_order`
--

DROP TABLE IF EXISTS `ec_stocktake_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_stocktake_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐩樼偣鍗曚富閿?,
  `order_no` varchar(32) NOT NULL COMMENT '鐩樼偣鍗曞彿',
  `factory_id` bigint DEFAULT NULL COMMENT '鎵€灞炲伐鍘?,
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `stocktake_time` datetime DEFAULT NULL COMMENT '鐩樼偣鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_stocktake_order_no` (`order_no`),
  KEY `idx_ec_stocktake_order_status` (`status`),
  KEY `idx_ec_stocktake_order_factory` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鐩樼偣鍗?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_stocktake_order`
--

LOCK TABLES `ec_stocktake_order` WRITE;
/*!40000 ALTER TABLE `ec_stocktake_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_stocktake_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_stocktake_order_line`
--

DROP TABLE IF EXISTS `ec_stocktake_order_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_stocktake_order_line` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏄庣粏涓婚敭',
  `order_id` bigint NOT NULL COMMENT '鐩樼偣鍗?ID',
  `sku_code` varchar(64) NOT NULL COMMENT 'SKU 璐у彿',
  `book_quantity` int NOT NULL COMMENT '璐﹂潰鏁伴噺(淇濆瓨鏃跺揩鐓?',
  `actual_quantity` int DEFAULT NULL COMMENT '瀹炵洏鏁伴噺',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ec_stocktake_order_line_order` (`order_id`),
  KEY `idx_ec_stocktake_order_line_sku` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢鐩樼偣鍗曟槑缁?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_stocktake_order_line`
--

LOCK TABLES `ec_stocktake_order_line` WRITE;
/*!40000 ALTER TABLE `ec_stocktake_order_line` DISABLE KEYS */;
/*!40000 ALTER TABLE `ec_stocktake_order_line` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ec_system_config`
--

DROP TABLE IF EXISTS `ec_system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ec_system_config` (
  `config_key` varchar(64) NOT NULL COMMENT '閰嶇疆閿?inventory/order_import/express/delivery_note/company',
  `config_json` text NOT NULL COMMENT '閰嶇疆 JSON',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢靛晢绯荤粺鍙傛暟';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ec_system_config`
--

LOCK TABLES `ec_system_config` WRITE;
/*!40000 ALTER TABLE `ec_system_config` DISABLE KEYS */;
INSERT INTO `ec_system_config` VALUES ('company','{\"companyName\":\"\",\"address\":\"\",\"tel\":\"\",\"contactName\":\"\",\"contactPhone\":\"\"}','2026-06-29 17:33:08'),('delivery_note','{\"title\":\"鍞崄鍢夐€佽揣鍗昞",\"address\":\"\",\"tel\":\"\",\"preparedBy\":\"\",\"shipFromName\":\"\",\"shipFromPhone\":\"\",\"shipFromAddress\":\"\",\"requirementItems\":[],\"noteItems\":[]}','2026-06-29 17:33:08'),('express','{\"headerRow\":1,\"dataStartRow\":2,\"includeLabelPriceDefault\":false}','2026-06-29 17:33:08'),('inventory','{\"defaultAlertThreshold\":10,\"slowMovingDays\":45,\"slowMovingFallbackDays\":90}','2026-06-29 17:33:08'),('order_import','{\"headerRow\":1,\"dataStartRow\":2}','2026-06-29 17:33:08');
/*!40000 ALTER TABLE `ec_system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_baidu_pan_auth`
--

DROP TABLE IF EXISTS `nb_baidu_pan_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_baidu_pan_auth` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `user_id` bigint NOT NULL DEFAULT '1' COMMENT '鐢ㄦ埛 ID锛屽崟鐢ㄦ埛榛樿 1',
  `access_token` varchar(512) NOT NULL COMMENT '璁块棶浠ょ墝',
  `refresh_token` varchar(512) NOT NULL COMMENT '鍒锋柊浠ょ墝',
  `expires_at` datetime NOT NULL COMMENT 'access_token 杩囨湡鏃堕棿',
  `baidu_uid` bigint DEFAULT NULL COMMENT '鐧惧害鐢ㄦ埛 ID',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐧惧害缃戠洏 OAuth 鎺堟潈';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_baidu_pan_auth`
--

LOCK TABLES `nb_baidu_pan_auth` WRITE;
/*!40000 ALTER TABLE `nb_baidu_pan_auth` DISABLE KEYS */;
INSERT INTO `nb_baidu_pan_auth` VALUES (1,1,'121.db721214d1fb6c5dea7d548708777726.YlKTMhvONFRK91cH46tQMxyH_DIGmdl3Zwb-Exn.azMbRw','122.c0f53b77acaf97e6a8652d6e2512d14d.YaL0BB5N6u0XfDDZjsgVAAoqkWKCvFJFgqYax98.LOk_kQ','2026-07-24 17:30:20',NULL,0,'2026-06-24 17:30:20','2026-06-24 17:30:20');
/*!40000 ALTER TABLE `nb_baidu_pan_auth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_note`
--

DROP TABLE IF EXISTS `nb_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_note` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `notebook_id` bigint DEFAULT NULL COMMENT '鎵€灞炴枃浠跺す',
  `title` varchar(256) NOT NULL DEFAULT '' COMMENT '鏍囬',
  `storage_type` varchar(16) NOT NULL DEFAULT 'BAIDU_PAN' COMMENT 'BAIDU_PAN/LOCAL',
  `storage_path` varchar(512) NOT NULL DEFAULT '' COMMENT '缃戠洏鎴栨湰鍦扮浉瀵硅矾寰?,
  `storage_fs_id` bigint DEFAULT NULL COMMENT '鐧惧害 fs_id',
  `content_hash` char(64) DEFAULT NULL COMMENT 'SHA-256',
  `content_size` bigint NOT NULL DEFAULT '0' COMMENT '姝ｆ枃瀛楄妭鏁?,
  `content_version` int NOT NULL DEFAULT '1' COMMENT '涔愯閿佺増鏈?,
  `content_excerpt` varchar(512) DEFAULT NULL COMMENT '绾枃鏈憳瑕?,
  `sync_status` varchar(16) NOT NULL DEFAULT 'SYNCED' COMMENT 'SYNCING/SYNCED/FAILED',
  `sync_error` varchar(512) DEFAULT NULL COMMENT '鍚屾澶辫触鍘熷洜',
  `note_type` varchar(16) NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/TODO/MEMO',
  `is_pinned` tinyint NOT NULL DEFAULT '0' COMMENT '缃《',
  `is_favorite` tinyint NOT NULL DEFAULT '0' COMMENT '鏀惰棌',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `status` varchar(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notebook_id` (`notebook_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绗旇';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_note`
--

LOCK TABLES `nb_note` WRITE;
/*!40000 ALTER TABLE `nb_note` DISABLE KEYS */;
INSERT INTO `nb_note` VALUES (1,3,'Java绋嬪簭杩愯鍘熺悊','LOCAL','notes/1.html',NULL,'7f528a45064c240a475f2946cae647b51c7a54e348ef7d01bb5487e7e2e67f17',1519,3,'JVM 鍔犺浇銆侀獙 璇併€佸噯澶囥€佽В鏋愩€佸垵濮?鍖栤€﹀挸鍜冲挸 鑰虫俯鍘?鎴戝幓鍎跨鍖?鎾掓拻鏃︽硶甯堟墦鍙戞槸鐨勬硶甯堟墦鍙戜笁澶?,'SYNCED',NULL,'NOTE',0,0,1,'PUBLISHED',0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(2,4,'CPU缂撳瓨鍜屽唴瀛樺睆闅?,'BAIDU_PAN','',NULL,'5f8256f53c1985b8f02590639a469f2ae5c5ef428eb547e8505bf7bc86b9cb36',64,1,'volatile銆乻ynchronized 涓?happens-before鈥︾殑娉曞笀鎵?,'SYNCED',NULL,'NOTE',0,0,1,'PUBLISHED',0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(3,1,'鏃犳爣棰?,'LOCAL','notes/3.html',NULL,'',0,0,'','SYNCING',NULL,'NOTE',0,0,0,'PUBLISHED',1,'2026-06-24 22:35:58','2026-06-24 22:36:05'),(4,6,'涓€銆丼pring Ai Alibaba','LOCAL','notes/4.html',NULL,'eb624e26f4483be22b6216582053e64e3d800716fd6a6c991cba4adb5a560f8e',13,303,'璇曡瘯','SYNCED',NULL,'NOTE',0,0,0,'PUBLISHED',0,'2026-06-24 22:37:07','2026-06-24 22:37:07'),(5,6,'浜屻€丱llama鐨勬帴鍏?,'LOCAL','notes/5.html',NULL,'89a9c3cd76deeb7795f9be2233a81c15606d6c73bf4587abe79887cfaad87c00',11,76,'','SYNCED',NULL,'NOTE',0,0,1,'PUBLISHED',0,'2026-06-26 19:09:03','2026-06-26 19:09:03'),(6,6,'涓夈€丆hatClient','LOCAL','notes/6.html',NULL,'89a9c3cd76deeb7795f9be2233a81c15606d6c73bf4587abe79887cfaad87c00',11,104,'','SYNCED',NULL,'NOTE',0,0,2,'PUBLISHED',0,'2026-06-26 20:56:01','2026-06-26 20:56:01');
/*!40000 ALTER TABLE `nb_note` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_note_tag`
--

DROP TABLE IF EXISTS `nb_note_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_note_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `name` varchar(64) NOT NULL COMMENT '鏍囩鍚?,
  `color` varchar(16) DEFAULT NULL COMMENT '棰滆壊',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绗旇鏍囩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_note_tag`
--

LOCK TABLES `nb_note_tag` WRITE;
/*!40000 ALTER TABLE `nb_note_tag` DISABLE KEYS */;
INSERT INTO `nb_note_tag` VALUES (1,'Java','#409eff',0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(2,'Spring',NULL,0,'2026-06-24 22:38:35','2026-06-24 22:38:35'),(3,'AI',NULL,0,'2026-06-24 22:38:38','2026-06-24 22:38:38');
/*!40000 ALTER TABLE `nb_note_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_note_tag_rel`
--

DROP TABLE IF EXISTS `nb_note_tag_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_note_tag_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `note_id` bigint NOT NULL COMMENT '绗旇 ID',
  `tag_id` bigint NOT NULL COMMENT '鏍囩 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_tag` (`note_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=908 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绗旇-鏍囩鍏宠仈';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_note_tag_rel`
--

LOCK TABLES `nb_note_tag_rel` WRITE;
/*!40000 ALTER TABLE `nb_note_tag_rel` DISABLE KEYS */;
INSERT INTO `nb_note_tag_rel` VALUES (905,4,1),(906,4,2),(907,4,3);
/*!40000 ALTER TABLE `nb_note_tag_rel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_notebook`
--

DROP TABLE IF EXISTS `nb_notebook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_notebook` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `parent_id` bigint DEFAULT NULL COMMENT '鐖舵枃浠跺す ID锛孨ULL 涓烘牴绾?,
  `name` varchar(128) NOT NULL COMMENT '鏂囦欢澶瑰悕绉?,
  `icon` varchar(32) DEFAULT NULL COMMENT '鍥炬爣鏍囪瘑',
  `color` varchar(16) DEFAULT NULL COMMENT '棰滆壊',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绗旇鏈枃浠跺す';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_notebook`
--

LOCK TABLES `nb_notebook` WRITE;
/*!40000 ALTER TABLE `nb_notebook` DISABLE KEYS */;
INSERT INTO `nb_notebook` VALUES (1,NULL,'宸ヤ綔',NULL,NULL,1,0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(2,1,'java',NULL,NULL,1,0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(3,2,'鍩虹',NULL,NULL,1,0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(4,2,'杩涢樁',NULL,NULL,2,0,'2026-06-24 15:38:22','2026-06-24 15:38:22'),(5,1,'AI',NULL,NULL,2,0,'2026-06-24 22:36:47','2026-06-24 22:36:47'),(6,5,'SpringAiAlibaba',NULL,NULL,0,0,'2026-06-24 22:37:02','2026-06-24 22:37:02');
/*!40000 ALTER TABLE `nb_notebook` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nb_todo_item`
--

DROP TABLE IF EXISTS `nb_todo_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nb_todo_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `content` varchar(512) NOT NULL DEFAULT '' COMMENT '寰呭姙鍐呭',
  `completed` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁瀹屾垚 0/1',
  `due_time` datetime DEFAULT NULL COMMENT '鎴鏃堕棿',
  `remind_time` datetime DEFAULT NULL COMMENT '鎻愰啋鏃堕棿',
  `repeat_type` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
  `repeat_interval` int NOT NULL DEFAULT '1' COMMENT '閲嶅闂撮殧',
  `repeat_until` datetime DEFAULT NULL COMMENT '閲嶅鎴鏃ユ湡',
  `repeat_days` varchar(255) DEFAULT NULL COMMENT 'WEEKLY:1,3,5 MONTHLY:1,15 YEARLY:01-15,06-01',
  `remind_notified` tinyint NOT NULL DEFAULT '0' COMMENT '鎻愰啋鏄惁宸叉帹閫?0/1',
  `series_id` bigint DEFAULT NULL COMMENT '閲嶅绯诲垪 ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '鐗瑰埆鎻愰啋 0/1',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_completed` (`completed`),
  KEY `idx_due_time` (`due_time`),
  KEY `idx_remind_time` (`remind_time`),
  KEY `idx_series_id` (`series_id`),
  KEY `idx_pinned` (`pinned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绗旇鏈緟鍔?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nb_todo_item`
--

LOCK TABLES `nb_todo_item` WRITE;
/*!40000 ALTER TABLE `nb_todo_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `nb_todo_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pomodoro_plan`
--

DROP TABLE IF EXISTS `pomodoro_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pomodoro_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `title` varchar(128) NOT NULL COMMENT '璁″垝鍚嶇О',
  `work_duration_min` int NOT NULL DEFAULT '25' COMMENT '涓撴敞鏃堕暱(鍒嗛挓)',
  `short_break_min` int NOT NULL DEFAULT '5' COMMENT '鐭紤鎭?鍒嗛挓)',
  `long_break_min` int NOT NULL DEFAULT '15' COMMENT '闀夸紤鎭?鍒嗛挓)',
  `rounds_before_long_break` int NOT NULL DEFAULT '4' COMMENT '鍑犺疆鍚庨暱浼戞伅',
  `daily_goal_rounds` int NOT NULL DEFAULT '8' COMMENT '姣忔棩鐩爣杞',
  `daily_goal_minutes` int NOT NULL DEFAULT '200' COMMENT '姣忔棩鐩爣涓撴敞鍒嗛挓',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁榛樿璁″垝',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐣寗閽熻鍒?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pomodoro_plan`
--

LOCK TABLES `pomodoro_plan` WRITE;
/*!40000 ALTER TABLE `pomodoro_plan` DISABLE KEYS */;
INSERT INTO `pomodoro_plan` VALUES (1,'榛樿涓撴敞璁″垝',25,5,15,4,8,200,1,'ENABLED',0,'2026-06-24 15:38:22','2026-06-24 15:38:22');
/*!40000 ALTER TABLE `pomodoro_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pomodoro_record`
--

DROP TABLE IF EXISTS `pomodoro_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pomodoro_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `plan_id` bigint DEFAULT NULL COMMENT '鍏宠仈璁″垝',
  `record_type` varchar(16) NOT NULL COMMENT 'WORK/SHORT_BREAK/LONG_BREAK',
  `duration_sec` int NOT NULL COMMENT '瀹為檯鏃堕暱(绉?',
  `round_index` int NOT NULL DEFAULT '0' COMMENT '褰撴棩绗嚑杞笓娉?浠匴ORK鏈夋晥)',
  `stat_date` date NOT NULL COMMENT '缁熻鏃ユ湡',
  `source` varchar(16) NOT NULL DEFAULT 'ADMIN' COMMENT '鏉ユ簮 ADMIN/DEVICE',
  `remark` varchar(255) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐣寗閽熷畬鎴愯褰?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pomodoro_record`
--

LOCK TABLES `pomodoro_record` WRITE;
/*!40000 ALTER TABLE `pomodoro_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `pomodoro_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_import_batch`
--

DROP TABLE IF EXISTS `sys_import_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_import_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鎵规涓婚敭',
  `batch_no` varchar(32) NOT NULL COMMENT '鎵规鍙?,
  `profile_id` bigint DEFAULT NULL COMMENT '浣跨敤鐨勫鍏ラ厤缃?,
  `biz_type` varchar(32) NOT NULL COMMENT '涓氬姟绫诲瀷',
  `biz_context` json DEFAULT NULL COMMENT '涓氬姟涓婁笅鏂囷紝濡?{"shopId":1}',
  `file_name` varchar(256) DEFAULT NULL COMMENT '鍘熷鏂囦欢鍚?,
  `file_path` varchar(512) DEFAULT NULL COMMENT '瀛樺偍璺緞',
  `detected_columns` json DEFAULT NULL COMMENT '涓婁紶鏃舵娴嬪埌鐨勫垪鍚?,
  `source` varchar(16) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/SCHEDULED',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREVIEWED/COMMITTED/FAILED',
  `total_rows` int NOT NULL DEFAULT '0',
  `success_rows` int NOT NULL DEFAULT '0',
  `failed_rows` int NOT NULL DEFAULT '0',
  `unmatched_rows` int NOT NULL DEFAULT '0' COMMENT '涓氬姟灞傛湭鍖归厤琛屾暟',
  `error_summary` varchar(1024) DEFAULT NULL,
  `operator` varchar(64) DEFAULT NULL,
  `committed_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_import_batch_no` (`batch_no`),
  KEY `idx_sys_import_batch_biz` (`biz_type`),
  KEY `idx_sys_import_batch_profile` (`profile_id`),
  KEY `idx_sys_import_batch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='閫氱敤鏂囨。瀵煎叆鎵规';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_import_batch`
--

LOCK TABLES `sys_import_batch` WRITE;
/*!40000 ALTER TABLE `sys_import_batch` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_import_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_import_profile`
--

DROP TABLE IF EXISTS `sys_import_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_import_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '閰嶇疆涓婚敭',
  `name` varchar(128) NOT NULL COMMENT '閰嶇疆鍚嶇О',
  `biz_type` varchar(32) NOT NULL COMMENT '涓氬姟绫诲瀷 SALES_ORDER/...',
  `platform_id` bigint DEFAULT NULL COMMENT '缁戝畾骞冲彴 ec_platform.id锛堣鍗曞鍏ユ寜骞冲彴锛?,
  `scope_key` varchar(64) DEFAULT NULL COMMENT '浣滅敤鍩熼敭锛屽 platform:2',
  `shop_id` bigint DEFAULT NULL COMMENT '缁戝畾搴楅摵锛堝彲閫夛紝涓€鑸笉鐢級',
  `file_type` varchar(16) NOT NULL DEFAULT 'XLSX' COMMENT 'XLSX/XLS/CSV',
  `header_row` int NOT NULL DEFAULT '1' COMMENT '琛ㄥご琛屽彿锛?-based锛?,
  `data_start_row` int NOT NULL DEFAULT '2' COMMENT '鏁版嵁璧峰琛屽彿锛?-based锛?,
  `sheet_name` varchar(64) DEFAULT NULL COMMENT '宸ヤ綔琛ㄥ悕锛岀┖鍒欓涓?sheet',
  `column_mapping` json NOT NULL COMMENT '鍚庣瀛楁 -> 鏂囨。鍒楀悕',
  `value_mapping` json DEFAULT NULL COMMENT '鍊兼槧灏勶紝濡傚钩鍙扮姸鎬?-> 绯荤粺鐘舵€?,
  `extra_config` json DEFAULT NULL COMMENT '鎵╁睍閰嶇疆 JSON',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1鍚敤 0鍋滅敤',
  `remark` varchar(512) DEFAULT NULL COMMENT '澶囨敞',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_import_profile_biz_platform_name` (`biz_type`,`platform_id`,`name`),
  KEY `idx_sys_import_profile_biz` (`biz_type`),
  KEY `idx_sys_import_profile_platform` (`platform_id`),
  KEY `idx_sys_import_profile_shop` (`shop_id`),
  KEY `idx_sys_import_profile_scope` (`biz_type`,`scope_key`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='閫氱敤鏂囨。瀵煎叆鍒楁槧灏勯厤缃?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_import_profile`
--

LOCK TABLES `sys_import_profile` WRITE;
/*!40000 ALTER TABLE `sys_import_profile` DISABLE KEYS */;
INSERT INTO `sys_import_profile` VALUES (1,'1688excel妯＄増','SALES_ORDER',1,'platform:1',NULL,'XLSX',1,2,NULL,'{\"link_name\": \"璐у搧鏍囬\", \"buyer_name\": \"涔板\", \"order_time\": \"涓嬪崟鏃堕棿\", \"sku_quantity\": \"鏁伴噺\", \"sku_spec_name\": \"瑙勬牸\", \"platform_status\": \"璁㈠崟鐘舵€乗", \"receive_address\": \"鏀惰揣鍦板潃\", \"received_amount\": \"瀹炰粯娆綷", \"tracking_number\": \"杩愬崟鍙穃", \"platform_order_no\": \"璁㈠崟鍙穃", \"express_station_name\": \"鐗╂祦鍏徃\", \"platform_line_status\": \"閫€娆剧姸鎬乗"}','{\"宸插彂璐": \"SHIPPED\", \"宸插彇娑圽": \"CANCELLED\", \"宸插畬鎴怽": \"COMPLETED\", \"宸查€€娆綷": \"REFUNDED\", \"寰呭彂璐": \"PAID\", \"閫€娆句腑\": \"REFUNDED\", \"閫€娆炬垚鍔焅": \"REFUNDED\", \"閫€璐ч€€娆綷": \"RETURNED\", \"閮ㄥ垎閫€娆綷": \"PARTIAL_REFUND\"}','{\"defaultLineStatus\": \"PAID\"}',1,'1688 骞冲彴璁㈠崟瀵煎嚭榛樿鍒楁槧灏勶紝鍙寜瀹為檯瀵煎嚭鍒楀悕璋冩暣',0,'2026-06-24 15:38:29','2026-06-24 15:38:29'),(2,'娣樺疂excel妯＄増','SALES_ORDER',2,'platform:2',NULL,'XLSX',1,2,NULL,'{\"pay_time\": \"涔板浠樻鏃堕棿\", \"link_name\": \"瀹濊礉鏍囬\", \"ship_time\": \"鍙戣揣鏃堕棿\", \"buyer_name\": \"涔板浼氬憳鍚峔", \"order_time\": \"涔板涓嬪崟鏃堕棿\", \"buyer_phone\": \"鑱旂郴鎵嬫満\", \"sku_quantity\": \"瀹濊礉鎬绘暟閲廫", \"complete_time\": \"纭鏀惰揣鏃堕棿\", \"sku_spec_name\": \"瀹濊礉瑙勬牸\", \"platform_status\": \"璁㈠崟鐘舵€乗", \"receive_address\": \"鏀惰揣鍦板潃\", \"received_amount\": \"涔板瀹炰粯閲戦\", \"tracking_number\": \"杩愬崟鍙穃", \"platform_order_no\": \"璁㈠崟缂栧彿\", \"express_station_name\": \"鐗╂祦鍏徃\", \"platform_line_status\": \"璁㈠崟鐘舵€乗"}','{\"宸插彂璐": \"SHIPPED\", \"宸插彇娑圽": \"CANCELLED\", \"宸插畬鎴怽": \"COMPLETED\", \"宸查€€娆綷": \"REFUNDED\", \"寰呭彂璐": \"PAID\", \"浜ゆ槗鍏抽棴\": \"CANCELLED\", \"浜ゆ槗鎴愬姛\": \"COMPLETED\", \"閫€璐ч€€娆綷": \"RETURNED\", \"涔板宸蹭粯娆綷": \"PAID\", \"鍗栧宸插彂璐": \"SHIPPED\", \"绛夊緟涔板纭\": \"SHIPPED\", \"绛夊緟鍗栧鍙戣揣\": \"PAID\"}','{\"defaultLineStatus\": \"PAID\"}',1,'娣樺疂骞冲彴璁㈠崟瀵煎嚭榛樿鍒楁槧灏勶紝鍙寜瀹為檯瀵煎嚭鍒楀悕璋冩暣',0,'2026-06-24 15:38:29','2026-06-24 15:38:29');
/*!40000 ALTER TABLE `sys_import_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `username` varchar(64) NOT NULL COMMENT '鐧诲綍鍚?,
  `nickname` varchar(64) DEFAULT NULL COMMENT '鏄电О',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵€?,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绯荤粺鐢ㄦ埛';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','绠＄悊鍛?,'ENABLED',0,'2026-06-24 01:16:33','2026-06-24 01:16:33'),(2,'demo','婕旂ず鐢ㄦ埛','ENABLED',0,'2026-06-24 01:16:33','2026-06-24 01:16:33');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'ai_manager_admin'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-30 19:51:56
