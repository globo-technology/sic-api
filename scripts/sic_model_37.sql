-- MySQL dump 10.13  Distrib 5.7.21, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: ykcojs0liv7ir9od
-- ------------------------------------------------------
-- Server version	5.7.21-0ubuntu0.17.10.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `caja`
--

DROP TABLE IF EXISTS `caja`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `caja` (
  `id_Caja` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminada` bit(1) NOT NULL,
  `estado` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `fechaApertura` datetime NOT NULL,
  `fechaCierre` datetime DEFAULT NULL,
  `fechaCorteInforme` datetime NOT NULL,
  `nroCaja` int(11) NOT NULL,
  `observacion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `saldoFinal` decimal(25,15) DEFAULT NULL,
  `saldoInicial` decimal(25,15) DEFAULT NULL,
  `saldoReal` decimal(25,15) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  `id_UsuarioCierra` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Caja`),
  KEY `FK35s1jul15m6jtwahkes51y20g` (`id_Empresa`),
  KEY `FKficr9h9jpbgfrxqd0qps3b2g0` (`id_Usuario`),
  KEY `FKoix9ce0neb67xywd06dr7mmm7` (`id_UsuarioCierra`),
  CONSTRAINT `FK35s1jul15m6jtwahkes51y20g` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`),
  CONSTRAINT `FKficr9h9jpbgfrxqd0qps3b2g0` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKoix9ce0neb67xywd06dr7mmm7` FOREIGN KEY (`id_UsuarioCierra`) REFERENCES `usuario` (`id_Usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cliente`
--

DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cliente` (
  `id_Cliente` bigint(20) NOT NULL AUTO_INCREMENT,
  `contacto` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `direccion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `fechaAlta` datetime NOT NULL,
  `idFiscal` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `nombreFantasia` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `predeterminado` bit(1) NOT NULL,
  `razonSocial` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telPrimario` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telSecundario` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_CondicionIVA` bigint(20) DEFAULT NULL,
  `id_Usuario_Credencial` bigint(20) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Localidad` bigint(20) DEFAULT NULL,
  `id_Usuario_Viajante` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Cliente`),
  KEY `FKm5l8c91knfxk0w27btt6x3vro` (`id_CondicionIVA`),
  KEY `FK5ywv51athxmruj1bquhyxrik9` (`id_Usuario_Credencial`),
  KEY `FKahu5l6761ite2fsglie24w1bg` (`id_Empresa`),
  KEY `FKc6sfncrbiypm57rdsn5gdoffe` (`id_Localidad`),
  KEY `FK6y3sisf1mpjhm630u69e8ediq` (`id_Usuario_Viajante`),
  CONSTRAINT `FK5ywv51athxmruj1bquhyxrik9` FOREIGN KEY (`id_Usuario_Credencial`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FK6y3sisf1mpjhm630u69e8ediq` FOREIGN KEY (`id_Usuario_Viajante`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKahu5l6761ite2fsglie24w1bg` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`),
  CONSTRAINT `FKc6sfncrbiypm57rdsn5gdoffe` FOREIGN KEY (`id_Localidad`) REFERENCES `localidad` (`id_Localidad`),
  CONSTRAINT `FKm5l8c91knfxk0w27btt6x3vro` FOREIGN KEY (`id_CondicionIVA`) REFERENCES `condicioniva` (`id_CondicionIVA`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `condicioniva`
--

DROP TABLE IF EXISTS `condicioniva`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `condicioniva` (
  `id_CondicionIVA` bigint(20) NOT NULL AUTO_INCREMENT,
  `discriminaIVA` bit(1) NOT NULL,
  `eliminada` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id_CondicionIVA`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `configuraciondelsistema`
--

DROP TABLE IF EXISTS `configuraciondelsistema`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `configuraciondelsistema` (
  `id_ConfiguracionDelSistema` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidadMaximaDeRenglonesEnFactura` int(11) NOT NULL,
  `certificadoAfip` longblob,
  `facturaElectronicaHabilitada` bit(1) NOT NULL,
  `firmanteCertificadoAfip` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `nroPuntoDeVentaAfip` int(11) NOT NULL,
  `passwordCertificadoAfip` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `usarFacturaVentaPreImpresa` bit(1) NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_ConfiguracionDelSistema`),
  KEY `FKayhqfqt2o07rn0utsh6h057xe` (`id_Empresa`),
  CONSTRAINT `FKayhqfqt2o07rn0utsh6h057xe` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cuentacorriente`
--

DROP TABLE IF EXISTS `cuentacorriente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cuentacorriente` (
  `id_cuenta_corriente` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminada` bit(1) NOT NULL,
  `fechaApertura` datetime NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_cuenta_corriente`),
  KEY `FKs7jnro4dgqdaexbg57371xkr2` (`id_Empresa`),
  CONSTRAINT `FKs7jnro4dgqdaexbg57371xkr2` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cuentacorrientecliente`
--

DROP TABLE IF EXISTS `cuentacorrientecliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cuentacorrientecliente` (
  `id_cuenta_corriente` bigint(20) NOT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_cuenta_corriente`),
  KEY `FKd22urpjy2kegalnkrlyxm12qj` (`id_Cliente`),
  CONSTRAINT `FKd22urpjy2kegalnkrlyxm12qj` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`),
  CONSTRAINT `FKrnsol1nucdjs3vf1n2ydvply5` FOREIGN KEY (`id_cuenta_corriente`) REFERENCES `cuentacorriente` (`id_cuenta_corriente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cuentacorrienteproveedor`
--

DROP TABLE IF EXISTS `cuentacorrienteproveedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cuentacorrienteproveedor` (
  `id_cuenta_corriente` bigint(20) NOT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_cuenta_corriente`),
  KEY `FK4fj6mk1bmlbd9k160kwjap5xd` (`id_Proveedor`),
  CONSTRAINT `FK4fj6mk1bmlbd9k160kwjap5xd` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`),
  CONSTRAINT `FK4k3cgpnc8aqgaj24d4ax3lcan` FOREIGN KEY (`id_cuenta_corriente`) REFERENCES `cuentacorriente` (`id_cuenta_corriente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `empresa`
--

DROP TABLE IF EXISTS `empresa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `empresa` (
  `id_Empresa` bigint(20) NOT NULL AUTO_INCREMENT,
  `cuip` bigint(20) NOT NULL,
  `direccion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminada` bit(1) NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `fechaInicioActividad` datetime DEFAULT NULL,
  `ingresosBrutos` bigint(20) NOT NULL,
  `lema` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `logo` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telefono` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_CondicionIVA` bigint(20) DEFAULT NULL,
  `id_Localidad` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Empresa`),
  KEY `FKoe8ihwidpastxfeneq5k4vs07` (`id_CondicionIVA`),
  KEY `FK98yi7oddg1up58158pwk9lf39` (`id_Localidad`),
  CONSTRAINT `FK98yi7oddg1up58158pwk9lf39` FOREIGN KEY (`id_Localidad`) REFERENCES `localidad` (`id_Localidad`),
  CONSTRAINT `FKoe8ihwidpastxfeneq5k4vs07` FOREIGN KEY (`id_CondicionIVA`) REFERENCES `condicioniva` (`id_CondicionIVA`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `factura`
--

DROP TABLE IF EXISTS `factura`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `factura` (
  `id_Factura` bigint(20) NOT NULL AUTO_INCREMENT,
  `CAE` bigint(20) NOT NULL,
  `descuento_neto` decimal(25,15) DEFAULT NULL,
  `descuento_porcentaje` decimal(25,15) DEFAULT NULL,
  `eliminada` bit(1) NOT NULL,
  `fecha` datetime NOT NULL,
  `fechaVencimiento` datetime DEFAULT NULL,
  `impuestoInterno_neto` decimal(25,15) DEFAULT NULL,
  `iva_105_neto` decimal(25,15) DEFAULT NULL,
  `iva_21_neto` decimal(25,15) DEFAULT NULL,
  `numFactura` bigint(20) NOT NULL,
  `numFacturaAfip` bigint(20) NOT NULL,
  `numSerie` bigint(20) NOT NULL,
  `numSerieAfip` bigint(20) NOT NULL,
  `observaciones` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `recargo_neto` decimal(25,15) DEFAULT NULL,
  `recargo_porcentaje` decimal(25,15) DEFAULT NULL,
  `subTotal` decimal(25,15) DEFAULT NULL,
  `subTotal_bruto` decimal(25,15) DEFAULT NULL,
  `tipoComprobante` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `total` decimal(25,15) DEFAULT NULL,
  `vencimientoCAE` datetime DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Pedido` bigint(20) DEFAULT NULL,
  `id_Transportista` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Factura`),
  KEY `FKqfqpih8u3cnw8u1px9jowvhm4` (`id_Empresa`),
  KEY `FKc7e4t8aids9o8jdlxf49vq7rd` (`id_Pedido`),
  KEY `FK109ik0d0amc3qr5tncsyvueb5` (`id_Transportista`),
  CONSTRAINT `FK109ik0d0amc3qr5tncsyvueb5` FOREIGN KEY (`id_Transportista`) REFERENCES `transportista` (`id_Transportista`),
  CONSTRAINT `FKc7e4t8aids9o8jdlxf49vq7rd` FOREIGN KEY (`id_Pedido`) REFERENCES `pedido` (`id_Pedido`),
  CONSTRAINT `FKqfqpih8u3cnw8u1px9jowvhm4` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `facturacompra`
--

DROP TABLE IF EXISTS `facturacompra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `facturacompra` (
  `id_Factura` bigint(20) NOT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Factura`),
  KEY `FKq775l11eckpnx4cawgw7dxlt` (`id_Proveedor`),
  CONSTRAINT `FKq775l11eckpnx4cawgw7dxlt` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`),
  CONSTRAINT `FKt4en2kmvrfdko3l3a5vd5aeai` FOREIGN KEY (`id_Factura`) REFERENCES `factura` (`id_Factura`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `facturaventa`
--

DROP TABLE IF EXISTS `facturaventa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `facturaventa` (
  `id_Factura` bigint(20) NOT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Factura`),
  KEY `FK3rq7obvimp12l50m06cyuvq48` (`id_Cliente`),
  KEY `FKr58rs6i7mo2ow1d09o5yxb7vk` (`id_Usuario`),
  CONSTRAINT `FK3rq7obvimp12l50m06cyuvq48` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`),
  CONSTRAINT `FK9wyj4hw8jmxpesc0j5o4iead4` FOREIGN KEY (`id_Factura`) REFERENCES `factura` (`id_Factura`),
  CONSTRAINT `FKr58rs6i7mo2ow1d09o5yxb7vk` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `formadepago`
--

DROP TABLE IF EXISTS `formadepago`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `formadepago` (
  `id_FormaDePago` bigint(20) NOT NULL AUTO_INCREMENT,
  `afectaCaja` bit(1) NOT NULL,
  `eliminada` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `predeterminado` bit(1) NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_FormaDePago`),
  KEY `FK6v6reo24igck98mwjs9b18c2j` (`id_Empresa`),
  CONSTRAINT `FK6v6reo24igck98mwjs9b18c2j` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gasto`
--

DROP TABLE IF EXISTS `gasto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gasto` (
  `id_Gasto` bigint(20) NOT NULL AUTO_INCREMENT,
  `concepto` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `fecha` datetime NOT NULL,
  `monto` decimal(25,15) DEFAULT NULL,
  `nroGasto` bigint(20) NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_FormaDePago` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Gasto`),
  KEY `FK7wdv6t0gyqi00myfynlcix3p4` (`id_Empresa`),
  KEY `FKp93smvff2cw9en7yxwil8m3tw` (`id_FormaDePago`),
  KEY `FKe6u9i1cjq5dyogni2c7ir1lwa` (`id_Usuario`),
  CONSTRAINT `FK7wdv6t0gyqi00myfynlcix3p4` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`),
  CONSTRAINT `FKe6u9i1cjq5dyogni2c7ir1lwa` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKp93smvff2cw9en7yxwil8m3tw` FOREIGN KEY (`id_FormaDePago`) REFERENCES `formadepago` (`id_FormaDePago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `itemcarritocompra`
--

DROP TABLE IF EXISTS `itemcarritocompra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `itemcarritocompra` (
  `idItemCarritoCompra` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(25,15) DEFAULT NULL,
  `importe` decimal(25,15) DEFAULT NULL,
  `id_Producto` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idItemCarritoCompra`),
  KEY `FKep0mqpdc2511a4kxjov4kowoa` (`id_Producto`),
  KEY `FK34ynexhgbnkf26hhcfy9wmcni` (`id_Usuario`),
  CONSTRAINT `FK34ynexhgbnkf26hhcfy9wmcni` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKep0mqpdc2511a4kxjov4kowoa` FOREIGN KEY (`id_Producto`) REFERENCES `producto` (`id_Producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `localidad`
--

DROP TABLE IF EXISTS `localidad`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `localidad` (
  `id_Localidad` bigint(20) NOT NULL AUTO_INCREMENT,
  `codigoPostal` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminada` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_Provincia` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Localidad`),
  KEY `FKip25qf9bb8vuf96trysbnng6g` (`id_Provincia`),
  CONSTRAINT `FKip25qf9bb8vuf96trysbnng6g` FOREIGN KEY (`id_Provincia`) REFERENCES `provincia` (`id_Provincia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medida`
--

DROP TABLE IF EXISTS `medida`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `medida` (
  `id_Medida` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminada` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Medida`),
  KEY `FK5jsf5bmdsydn5wfvlgsofl4vf` (`id_Empresa`),
  CONSTRAINT `FK5jsf5bmdsydn5wfvlgsofl4vf` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `nota`
--

DROP TABLE IF EXISTS `nota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nota` (
  `idNota` bigint(20) NOT NULL AUTO_INCREMENT,
  `CAE` bigint(20) NOT NULL,
  `eliminada` bit(1) NOT NULL,
  `fecha` datetime NOT NULL,
  `iva105Neto` decimal(25,15) DEFAULT NULL,
  `iva21Neto` decimal(25,15) DEFAULT NULL,
  `motivo` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `nroNota` bigint(20) NOT NULL,
  `numNotaAfip` bigint(20) NOT NULL,
  `numSerieAfip` bigint(20) NOT NULL,
  `serie` bigint(20) NOT NULL,
  `subTotalBruto` decimal(25,15) DEFAULT NULL,
  `tipoComprobante` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `total` decimal(25,15) DEFAULT NULL,
  `vencimientoCAE` datetime DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FKnt9tgtl6tq8pe0eq2o835lecb` (`id_Empresa`),
  KEY `FK3tyjyayu04yehvuohrj8u9ult` (`id_Usuario`),
  CONSTRAINT `FK3tyjyayu04yehvuohrj8u9ult` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKnt9tgtl6tq8pe0eq2o835lecb` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notacredito`
--

DROP TABLE IF EXISTS `notacredito`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notacredito` (
  `descuentoNeto` decimal(25,15) DEFAULT NULL,
  `descuentoPorcentaje` decimal(25,15) DEFAULT NULL,
  `modificaStock` bit(1) NOT NULL,
  `recargoNeto` decimal(25,15) DEFAULT NULL,
  `recargoPorcentaje` decimal(25,15) DEFAULT NULL,
  `subTotal` decimal(25,15) DEFAULT NULL,
  `idNota` bigint(20) NOT NULL,
  PRIMARY KEY (`idNota`),
  CONSTRAINT `FKmil73ynt2hdoi5k40lhnfuc0p` FOREIGN KEY (`idNota`) REFERENCES `nota` (`idNota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notacreditocliente`
--

DROP TABLE IF EXISTS `notacreditocliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notacreditocliente` (
  `idNota` bigint(20) NOT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  `id_Factura` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FK7xeray8mjw2jgtl21c5tweq19` (`id_Cliente`),
  KEY `FK4ogxai4el1w87fkrpxeggtw64` (`id_Factura`),
  CONSTRAINT `FK4ogxai4el1w87fkrpxeggtw64` FOREIGN KEY (`id_Factura`) REFERENCES `facturaventa` (`id_Factura`),
  CONSTRAINT `FK7xeray8mjw2jgtl21c5tweq19` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`),
  CONSTRAINT `FK8op21x39y5rbao3nu1vws0uti` FOREIGN KEY (`idNota`) REFERENCES `notacredito` (`idNota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notacreditoproveedor`
--

DROP TABLE IF EXISTS `notacreditoproveedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notacreditoproveedor` (
  `idNota` bigint(20) NOT NULL,
  `id_Factura` bigint(20) DEFAULT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FK9sr9nlgovxhbrka8mkwvb4omn` (`id_Factura`),
  KEY `FKbt67q5al5vnfafh95mtch6l72` (`id_Proveedor`),
  CONSTRAINT `FK2450n01qle6djbk68r1epo1f9` FOREIGN KEY (`idNota`) REFERENCES `notacredito` (`idNota`),
  CONSTRAINT `FK9sr9nlgovxhbrka8mkwvb4omn` FOREIGN KEY (`id_Factura`) REFERENCES `facturacompra` (`id_Factura`),
  CONSTRAINT `FKbt67q5al5vnfafh95mtch6l72` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notadebito`
--

DROP TABLE IF EXISTS `notadebito`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notadebito` (
  `montoNoGravado` decimal(25,15) DEFAULT NULL,
  `idNota` bigint(20) NOT NULL,
  `idRecibo` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FKbqw43bhcftd73q9oy97tnvitg` (`idRecibo`),
  CONSTRAINT `FK62ggkmr3ghilmo5awdyqvdu5f` FOREIGN KEY (`idNota`) REFERENCES `nota` (`idNota`),
  CONSTRAINT `FKbqw43bhcftd73q9oy97tnvitg` FOREIGN KEY (`idRecibo`) REFERENCES `recibo` (`idRecibo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notadebitocliente`
--

DROP TABLE IF EXISTS `notadebitocliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notadebitocliente` (
  `idNota` bigint(20) NOT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FKkvtfwp7euy13x8mht6h3ghw79` (`id_Cliente`),
  CONSTRAINT `FKch0mx6qe31sm0lclo724j7l1t` FOREIGN KEY (`idNota`) REFERENCES `notadebito` (`idNota`),
  CONSTRAINT `FKkvtfwp7euy13x8mht6h3ghw79` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notadebitoproveedor`
--

DROP TABLE IF EXISTS `notadebitoproveedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notadebitoproveedor` (
  `idNota` bigint(20) NOT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idNota`),
  KEY `FK6xy7r9oi12919afp9w6d865h6` (`id_Proveedor`),
  CONSTRAINT `FK215qg7lw2yxfn2n87ylelwt20` FOREIGN KEY (`idNota`) REFERENCES `notadebito` (`idNota`),
  CONSTRAINT `FK6xy7r9oi12919afp9w6d865h6` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pais`
--

DROP TABLE IF EXISTS `pais`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pais` (
  `id_Pais` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminado` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id_Pais`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pedido`
--

DROP TABLE IF EXISTS `pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pedido` (
  `id_Pedido` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminado` bit(1) NOT NULL,
  `estado` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `fecha` datetime NOT NULL,
  `fechaVencimiento` datetime DEFAULT NULL,
  `nroPedido` bigint(20) NOT NULL,
  `observaciones` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `totalActual` decimal(25,15) DEFAULT NULL,
  `totalEstimado` decimal(25,15) DEFAULT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Pedido`),
  KEY `FKldu89sbi8gpn3n13rlj8gbq94` (`id_Cliente`),
  KEY `FKrwecn3anida2fmxejg1yqn62l` (`id_Empresa`),
  KEY `FKhi0qk154awxum75vbi0rfkwe7` (`id_Usuario`),
  CONSTRAINT `FKhi0qk154awxum75vbi0rfkwe7` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKldu89sbi8gpn3n13rlj8gbq94` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`),
  CONSTRAINT `FKrwecn3anida2fmxejg1yqn62l` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `producto`
--

DROP TABLE IF EXISTS `producto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `producto` (
  `id_Producto` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantMinima` decimal(25,15) DEFAULT NULL,
  `cantidad` decimal(25,15) DEFAULT NULL,
  `codigo` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `estante` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `estanteria` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `fechaAlta` datetime NOT NULL,
  `fechaUltimaModificacion` datetime NOT NULL,
  `fechaVencimiento` datetime DEFAULT NULL,
  `ganancia_neto` decimal(25,15) DEFAULT NULL,
  `ganancia_porcentaje` decimal(25,15) DEFAULT NULL,
  `ilimitado` bit(1) NOT NULL,
  `impuestoInterno_neto` decimal(25,15) DEFAULT NULL,
  `impuestoInterno_porcentaje` decimal(25,15) DEFAULT NULL,
  `iva_neto` decimal(25,15) DEFAULT NULL,
  `iva_porcentaje` decimal(25,15) DEFAULT NULL,
  `nota` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `precioCosto` decimal(25,15) DEFAULT NULL,
  `precioLista` decimal(25,15) DEFAULT NULL,
  `precioVentaPublico` decimal(25,15) DEFAULT NULL,
  `ventaMinima` decimal(25,15) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Medida` bigint(20) DEFAULT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  `id_Rubro` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Producto`),
  KEY `FKmicsquyd17liutvxtw6uao7fo` (`id_Empresa`),
  KEY `FK3gu6hhvw1rjp04xmv3iojobov` (`id_Medida`),
  KEY `FKl34ci72jii8lbp9swh6rdxypg` (`id_Proveedor`),
  KEY `FKlu9bcwngart3rui0r4laq8n1b` (`id_Rubro`),
  CONSTRAINT `FK3gu6hhvw1rjp04xmv3iojobov` FOREIGN KEY (`id_Medida`) REFERENCES `medida` (`id_Medida`),
  CONSTRAINT `FKl34ci72jii8lbp9swh6rdxypg` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`),
  CONSTRAINT `FKlu9bcwngart3rui0r4laq8n1b` FOREIGN KEY (`id_Rubro`) REFERENCES `rubro` (`id_Rubro`),
  CONSTRAINT `FKmicsquyd17liutvxtw6uao7fo` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `proveedor`
--

DROP TABLE IF EXISTS `proveedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proveedor` (
  `id_Proveedor` bigint(20) NOT NULL AUTO_INCREMENT,
  `codigo` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `contacto` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `direccion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `idFiscal` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `razonSocial` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telPrimario` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telSecundario` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `web` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_CondicionIVA` bigint(20) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Localidad` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Proveedor`),
  KEY `FK4hiu7610oh99ykb29eale9pg9` (`id_CondicionIVA`),
  KEY `FK5s5a4d2763thtum39ht6r059q` (`id_Empresa`),
  KEY `FK93qeca10ljkj4qmj59yyp11of` (`id_Localidad`),
  CONSTRAINT `FK4hiu7610oh99ykb29eale9pg9` FOREIGN KEY (`id_CondicionIVA`) REFERENCES `condicioniva` (`id_CondicionIVA`),
  CONSTRAINT `FK5s5a4d2763thtum39ht6r059q` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`),
  CONSTRAINT `FK93qeca10ljkj4qmj59yyp11of` FOREIGN KEY (`id_Localidad`) REFERENCES `localidad` (`id_Localidad`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `provincia`
--

DROP TABLE IF EXISTS `provincia`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provincia` (
  `id_Provincia` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminada` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_Pais` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Provincia`),
  KEY `FKoeyy00k8sswpaedo6i6dvux4r` (`id_Pais`),
  CONSTRAINT `FKoeyy00k8sswpaedo6i6dvux4r` FOREIGN KEY (`id_Pais`) REFERENCES `pais` (`id_Pais`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recibo`
--

DROP TABLE IF EXISTS `recibo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `recibo` (
  `idRecibo` bigint(20) NOT NULL AUTO_INCREMENT,
  `concepto` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `fecha` datetime NOT NULL,
  `monto` decimal(25,15) DEFAULT NULL,
  `numRecibo` bigint(20) NOT NULL,
  `numSerie` bigint(20) NOT NULL,
  `id_Cliente` bigint(20) DEFAULT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_FormaDePago` bigint(20) DEFAULT NULL,
  `id_Proveedor` bigint(20) DEFAULT NULL,
  `id_Usuario` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idRecibo`),
  KEY `FKg0ktcrcla69by0fw5jov45bqa` (`id_Cliente`),
  KEY `FKn3pouxafigal4oo25r16sp5au` (`id_Empresa`),
  KEY `FKfluiumc3d4nc4swosx8isd279` (`id_FormaDePago`),
  KEY `FKsbq4xq9oehrhje6f5m418qgdq` (`id_Proveedor`),
  KEY `FK7xcubmytbr5c4xcdooqok230s` (`id_Usuario`),
  CONSTRAINT `FK7xcubmytbr5c4xcdooqok230s` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`),
  CONSTRAINT `FKfluiumc3d4nc4swosx8isd279` FOREIGN KEY (`id_FormaDePago`) REFERENCES `formadepago` (`id_FormaDePago`),
  CONSTRAINT `FKg0ktcrcla69by0fw5jov45bqa` FOREIGN KEY (`id_Cliente`) REFERENCES `cliente` (`id_Cliente`),
  CONSTRAINT `FKn3pouxafigal4oo25r16sp5au` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`),
  CONSTRAINT `FKsbq4xq9oehrhje6f5m418qgdq` FOREIGN KEY (`id_Proveedor`) REFERENCES `proveedor` (`id_Proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rengloncuentacorriente`
--

DROP TABLE IF EXISTS `rengloncuentacorriente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rengloncuentacorriente` (
  `id_renglon_cuenta_corriente` bigint(20) NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `eliminado` bit(1) NOT NULL,
  `fecha` datetime NOT NULL,
  `fechaVencimiento` datetime DEFAULT NULL,
  `idMovimiento` bigint(20) NOT NULL,
  `monto` decimal(25,15) DEFAULT NULL,
  `numero` bigint(20) NOT NULL,
  `serie` bigint(20) NOT NULL,
  `tipo_comprobante` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_cuenta_corriente` bigint(20) DEFAULT NULL,
  `id_Factura` bigint(20) DEFAULT NULL,
  `idNota` bigint(20) DEFAULT NULL,
  `idRecibo` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_renglon_cuenta_corriente`),
  KEY `FKnmr7gra9hmxv8s8odm6dy6eg9` (`id_cuenta_corriente`),
  KEY `FKkw1g401txuln3k21xs8q7ypbq` (`id_Factura`),
  KEY `FKse5m5s33xj59pg8xyxatxn4jc` (`idNota`),
  KEY `FKnc57ol526tmqxysywvfys8pxx` (`idRecibo`),
  CONSTRAINT `FKkw1g401txuln3k21xs8q7ypbq` FOREIGN KEY (`id_Factura`) REFERENCES `factura` (`id_Factura`),
  CONSTRAINT `FKnc57ol526tmqxysywvfys8pxx` FOREIGN KEY (`idRecibo`) REFERENCES `recibo` (`idRecibo`),
  CONSTRAINT `FKnmr7gra9hmxv8s8odm6dy6eg9` FOREIGN KEY (`id_cuenta_corriente`) REFERENCES `cuentacorriente` (`id_cuenta_corriente`),
  CONSTRAINT `FKse5m5s33xj59pg8xyxatxn4jc` FOREIGN KEY (`idNota`) REFERENCES `nota` (`idNota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `renglonfactura`
--

DROP TABLE IF EXISTS `renglonfactura`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `renglonfactura` (
  `id_RenglonFactura` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(25,15) DEFAULT NULL,
  `codigoItem` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `descripcionItem` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `descuento_neto` decimal(25,15) DEFAULT NULL,
  `descuento_porcentaje` decimal(25,15) DEFAULT NULL,
  `ganancia_neto` decimal(25,15) DEFAULT NULL,
  `ganancia_porcentaje` decimal(25,15) DEFAULT NULL,
  `id_ProductoItem` bigint(20) NOT NULL,
  `importe` decimal(25,15) DEFAULT NULL,
  `impuesto_neto` decimal(25,15) DEFAULT NULL,
  `impuesto_porcentaje` decimal(25,15) DEFAULT NULL,
  `iva_neto` decimal(25,15) DEFAULT NULL,
  `iva_porcentaje` decimal(25,15) DEFAULT NULL,
  `medidaItem` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `precioUnitario` decimal(25,15) DEFAULT NULL,
  `id_Factura` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_RenglonFactura`),
  KEY `FKk4jqfq7oxfmdr37ttt1s9f9e6` (`id_Factura`),
  CONSTRAINT `FKk4jqfq7oxfmdr37ttt1s9f9e6` FOREIGN KEY (`id_Factura`) REFERENCES `factura` (`id_Factura`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `renglonnotacredito`
--

DROP TABLE IF EXISTS `renglonnotacredito`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `renglonnotacredito` (
  `idRenglonNotaCredito` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(25,15) DEFAULT NULL,
  `codigoItem` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `descripcionItem` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `descuentoNeto` decimal(25,15) DEFAULT NULL,
  `descuentoPorcentaje` decimal(25,15) DEFAULT NULL,
  `gananciaNeto` decimal(25,15) DEFAULT NULL,
  `gananciaPorcentaje` decimal(25,15) DEFAULT NULL,
  `idProductoItem` bigint(20) NOT NULL,
  `importe` decimal(25,15) DEFAULT NULL,
  `importeBruto` decimal(25,15) DEFAULT NULL,
  `importeNeto` decimal(25,15) DEFAULT NULL,
  `ivaNeto` decimal(25,15) DEFAULT NULL,
  `ivaPorcentaje` decimal(25,15) DEFAULT NULL,
  `medidaItem` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `precioUnitario` decimal(25,15) DEFAULT NULL,
  `idNota` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idRenglonNotaCredito`),
  KEY `FKokev3j3rqj18enm9llgyxejaf` (`idNota`),
  CONSTRAINT `FKokev3j3rqj18enm9llgyxejaf` FOREIGN KEY (`idNota`) REFERENCES `notacredito` (`idNota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `renglonnotadebito`
--

DROP TABLE IF EXISTS `renglonnotadebito`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `renglonnotadebito` (
  `idRenglonNotaDebito` bigint(20) NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `importeBruto` decimal(25,15) DEFAULT NULL,
  `importeNeto` decimal(25,15) DEFAULT NULL,
  `ivaNeto` decimal(25,15) DEFAULT NULL,
  `ivaPorcentaje` decimal(25,15) DEFAULT NULL,
  `monto` decimal(25,15) DEFAULT NULL,
  `idNota` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`idRenglonNotaDebito`),
  KEY `FKks965c8m6jb8pw2rq721tfq09` (`idNota`),
  CONSTRAINT `FKks965c8m6jb8pw2rq721tfq09` FOREIGN KEY (`idNota`) REFERENCES `notadebito` (`idNota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `renglonpedido`
--

DROP TABLE IF EXISTS `renglonpedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `renglonpedido` (
  `id_RenglonPedido` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(25,15) DEFAULT NULL,
  `descuento_neto` decimal(25,15) DEFAULT NULL,
  `descuento_porcentaje` decimal(25,15) DEFAULT NULL,
  `subTotal` decimal(25,15) DEFAULT NULL,
  `id_Producto` bigint(20) DEFAULT NULL,
  `id_Pedido` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_RenglonPedido`),
  KEY `FKfxncx6f7eg8swxchd6sllrssa` (`id_Producto`),
  KEY `FKtjjxjf88fwccfduk8hhf7q3pd` (`id_Pedido`),
  CONSTRAINT `FKfxncx6f7eg8swxchd6sllrssa` FOREIGN KEY (`id_Producto`) REFERENCES `producto` (`id_Producto`),
  CONSTRAINT `FKtjjxjf88fwccfduk8hhf7q3pd` FOREIGN KEY (`id_Pedido`) REFERENCES `pedido` (`id_Pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rol` (
  `id_Usuario` bigint(20) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  KEY `FKipmnlfqw19kwfv94oex7ygl5s` (`id_Usuario`),
  CONSTRAINT `FKipmnlfqw19kwfv94oex7ygl5s` FOREIGN KEY (`id_Usuario`) REFERENCES `usuario` (`id_Usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rubro`
--

DROP TABLE IF EXISTS `rubro`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rubro` (
  `id_Rubro` bigint(20) NOT NULL AUTO_INCREMENT,
  `eliminado` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Rubro`),
  KEY `FKjqodxje0wqn40nptfj4sij5al` (`id_Empresa`),
  CONSTRAINT `FKjqodxje0wqn40nptfj4sij5al` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transportista`
--

DROP TABLE IF EXISTS `transportista`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transportista` (
  `id_Transportista` bigint(20) NOT NULL AUTO_INCREMENT,
  `direccion` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `telefono` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `web` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `id_Empresa` bigint(20) DEFAULT NULL,
  `id_Localidad` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id_Transportista`),
  KEY `FKphhgo5taxw9nhjkav8ei6b6y9` (`id_Empresa`),
  KEY `FK7i066mrrg36mr0olx1eaqbua5` (`id_Localidad`),
  CONSTRAINT `FK7i066mrrg36mr0olx1eaqbua5` FOREIGN KEY (`id_Localidad`) REFERENCES `localidad` (`id_Localidad`),
  CONSTRAINT `FKphhgo5taxw9nhjkav8ei6b6y9` FOREIGN KEY (`id_Empresa`) REFERENCES `empresa` (`id_Empresa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usuario` (
  `id_Usuario` bigint(20) NOT NULL AUTO_INCREMENT,
  `apellido` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `eliminado` bit(1) NOT NULL,
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `habilitado` bit(1) NOT NULL,
  `nombre` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `passwordRecoveryKey` bigint(20) NOT NULL,
  `token` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id_Usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-03-18 21:35:03