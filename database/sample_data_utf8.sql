-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: ebs
-- ------------------------------------------------------
-- Server version	8.0.43

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
-- Dumping data for table `bill`
--

LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES ('CUST001','January','120','456.00','Paid'),('CUST001','February','135','498.75','Not Paid'),('MET001','January','180','663.00','Paid'),('MET001','February','165','607.25','Not Paid'),('MET002','January','200','738.00','Not Paid'),('MET002','February','190','701.50','Paid'),('MET003','January','145','534.25','Paid'),('MET003','February','160','589.00','Not Paid'),('MET004','January','175','644.25','Not Paid'),('MET004','February','185','681.25','Paid'),('MET005','January','220','811.00','Commercial'),('MET005','February','235','866.25','Commercial'),('MET006','January','110','405.50','Paid'),('MET006','February','125','460.25','Not Paid'),('MET007','January','195','719.25','Not Paid'),('MET007','February','205','756.25','Paid'),('MET008','January','170','626.50','Paid'),('MET008','February','155','571.25','Not Paid');
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES ('Customer','CUST001','123 Customer Street','Demo City','Demo State','customer@demo.com','555-DEMO'),('Rajesh Kumar Sharma','MET001','A-101 Shanti Nagar','Mumbai','Maharashtra','rajesh.sharma@email.com','9876543210'),('Priya Singh','MET002','B-205 Indira Colony','Delhi','Delhi','priya.singh@email.com','9876543211'),('Amit Patel','MET003','C-301 Sardar Society','Ahmedabad','Gujarat','amit.patel@email.com','9876543212'),('Sunita Gupta','MET004','D-15 Laxmi Nagar','Jaipur','Rajasthan','sunita.gupta@email.com','9876543213'),('Vikram Reddy','MET005','E-22 Banjara Hills','Hyderabad','Telangana','vikram.reddy@email.com','9876543214'),('Kavya Menon','MET006','F-18 Marine Drive','Kochi','Kerala','kavya.menon@email.com','9876543215'),('Arjun Yadav','MET007','G-33 Civil Lines','Lucknow','Uttar Pradesh','arjun.yadav@email.com','9876543216'),('Deepika Joshi','MET008','H-12 Model Town','Chandigarh','Punjab','deepika.joshi@email.com','9876543217');
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `login`
--

LOCK TABLES `login` WRITE;
/*!40000 ALTER TABLE `login` DISABLE KEYS */;
INSERT INTO `login` VALUES ('ADMIN001','Parth','Parth','Parth123','Admin'),('CUST001','Customer','Customer','Customer123','Customer'),('MET001','rajesh2024','Rajesh Kumar Sharma','raj@123','Customer'),('MET002','priya2024','Priya Singh','pri@123','Customer'),('MET003','amit2024','Amit Patel','amt@123','Customer'),('MET004','sunita2024','Sunita Gupta','sun@123','Customer'),('MET005','vikram2024','Vikram Reddy','vik@123','Customer'),('MET007','arjun2024','Arjun Yadav','arj@123','Customer'),('MET008','deepika2024','Deepika Joshi','dee@123','Customer'),('MET009','ram2024','Ram Jawan','Raam@123','Customer');
/*!40000 ALTER TABLE `login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `meter_info`
--

LOCK TABLES `meter_info` WRITE;
/*!40000 ALTER TABLE `meter_info` DISABLE KEYS */;
INSERT INTO `meter_info` VALUES ('CUST001','Main Gate','Digital','PC-001','Residential','30'),('MET001','Building Entrance','Digital','PC-002','Residential','30'),('MET002','Terrace','Smart Meter','PC-003','Residential','30'),('MET003','Ground Floor','Digital','PC-004','Residential','30'),('MET004','Front Yard','Smart Meter','PC-005','Residential','30'),('MET005','Side Wall','Digital','PC-006','Commercial','30'),('MET006','Main Door','Smart Meter','PC-007','Residential','30'),('MET007','Parking Area','Digital','PC-008','Residential','30'),('MET008','Garden','Smart Meter','PC-009','Residential','30');
/*!40000 ALTER TABLE `meter_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tax`
--

LOCK TABLES `tax` WRITE;
/*!40000 ALTER TABLE `tax` DISABLE KEYS */;
INSERT INTO `tax` VALUES (3.00,50.00,20.00,15.00,25.00);
/*!40000 ALTER TABLE `tax` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-08  2:16:07
