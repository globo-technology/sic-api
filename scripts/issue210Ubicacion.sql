CREATE TABLE `ubicacion` (
  `idUbicacion` bigint(20) NOT NULL AUTO_INCREMENT,
  `calle` varchar(255) COLLATE utf8_unicode_ci,
  `departamento` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `descripcion` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `latitud` double DEFAULT NULL,
  `longitud` double DEFAULT NULL,
  `numero` int(11) NOT NULL,
  `piso` int(11) DEFAULT NULL,
  `id_Localidad` bigint(20)  NOT NULL,
  `id_Cliente` bigint(20) NOT NULL,
  PRIMARY KEY (`idUbicacion`),
  KEY `FKnt928a1oc3mtwe2spg8ov6qdm` (`id_Localidad`),
  CONSTRAINT `FKnt928a1oc3mtwe2spg8ov6qdm` FOREIGN KEY (`id_Localidad`) REFERENCES `localidad` (`id_Localidad`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


INSERT INTO `ubicacion`(calle, numero, id_Localidad, id_Cliente) 
select "ubicacionFacturacion", 0,localidad.id_Localidad, cliente.id_Cliente from cliente inner join localidad
on cliente.id_Localidad = localidad.id_Localidad;

INSERT INTO `ubicacion`(calle, numero, id_Localidad, id_Cliente) 
select "ubicacionFacturacion", 0, 242, cliente.id_Cliente from cliente
where cliente.id_Localidad is null;

INSERT INTO `ubicacion`(calle, numero, id_Localidad, id_Cliente) 
select "ubicacionEnvio", 0,localidad.id_Localidad, cliente.id_Cliente from cliente inner join localidad
on cliente.id_Localidad = localidad.id_Localidad;

INSERT INTO `ubicacion`(calle, numero, id_Localidad, id_Cliente) 
select "ubicacionEnvio", 0, 242, cliente.id_Cliente from cliente
where cliente.id_Localidad is null;

ALTER TABLE `cliente` 
ADD COLUMN `idUbicacionFacturacion` BIGINT(20) AFTER `bonificacion`;

ALTER TABLE `cliente` 
ADD COLUMN `idUbicacionEnvio` BIGINT(20) AFTER `idUbicacionFacturacion`;

UPDATE cliente c
INNER JOIN ubicacion u ON c.id_Cliente = u.id_Cliente
SET c.idUbicacionFacturacion = u.idUbicacion
where u.calle = "ubicacionFacturacion"; 

SET SQL_SAFE_UPDATES = 0;
UPDATE ubicacion u inner join cliente c on u.id_Cliente = c.id_Cliente
SET u.descripcion = c.direccion;
SET SQL_SAFE_UPDATES = 1;

UPDATE cliente c
INNER JOIN ubicacion u ON c.id_Cliente = u.id_Cliente
SET c.idUbicacionEnvio = u.idUbicacion
where u.calle = "ubicacionEnvio"; 

SET foreign_key_checks = 0;

ALTER TABLE `cliente`
ADD CONSTRAINT `FK838frolnqaeg8h97ggqu1rd67` FOREIGN KEY (`idUbicacionFacturacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `cliente`
ADD CONSTRAINT `FKkfnh6um3l9l5i0ywxwqr1qq9e` FOREIGN KEY (`idUbicacionEnvio`) 
REFERENCES `ubicacion`(`idUbicacion`);

SET foreign_key_checks = 1;

ALTER TABLE `ubicacion` 
DROP COLUMN `id_Cliente`;

ALTER TABLE `cliente` 
DROP FOREIGN KEY `FKc6sfncrbiypm57rdsn5gdoffe`;
ALTER TABLE `cliente` 
DROP COLUMN `id_Localidad`,
DROP INDEX `FKc6sfncrbiypm57rdsn5gdoffe` ;

ALTER TABLE `cliente` 
DROP COLUMN `direccion`;

ALTER TABLE `localidad` 
ADD COLUMN  `costoEnvio` decimal(25,15) DEFAULT 0 NOT NULL,
ADD COLUMN  `envioGratuito` bit(1) DEFAULT 0 NOT NULL;

ALTER TABLE `localidad` 
ADD COLUMN  `idProveedor` bigint(20) DEFAULT NULL;

-- PROVEEDOR
ALTER TABLE `ubicacion` 
ADD COLUMN `idProveedor` BIGINT(20);

INSERT INTO `ubicacion`(calle, numero, id_Localidad, idProveedor) 
select "ubicacion proveedor", 123,localidad.id_Localidad, proveedor.id_Proveedor from proveedor inner join localidad
on proveedor.id_Localidad = localidad.id_Localidad;

INSERT INTO `ubicacion`(calle, numero, id_Localidad, idProveedor) 
select "ubicacion proveedor", 123, 242, proveedor.id_Proveedor from proveedor
where proveedor.id_Localidad is null;

SET SQL_SAFE_UPDATES = 0;
UPDATE ubicacion u inner join proveedor p on u.idProveedor = p.id_Proveedor
SET u.descripcion = p.direccion;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE `proveedor` 
DROP COLUMN `direccion`;

ALTER TABLE `proveedor` 
ADD COLUMN `idUbicacion` BIGINT(20);

UPDATE proveedor p
INNER JOIN ubicacion u ON p.id_Proveedor = u.idProveedor
SET p.idUbicacion = u.idUbicacion; 

ALTER TABLE `proveedor` 
DROP FOREIGN KEY `FK93qeca10ljkj4qmj59yyp11of`;
ALTER TABLE `proveedor` 
DROP COLUMN `id_Localidad`,
DROP INDEX `FK93qeca10ljkj4qmj59yyp11of` ;

-- FOREING KEY PROVEEDOR
ALTER TABLE `proveedor`
ADD CONSTRAINT `FKjljtmiir6f667w008hwkpqoca` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `ubicacion` 
DROP COLUMN `idProveedor`;

ALTER TABLE `localidad` 
DROP COLUMN `idProveedor`;

-- Transportista

ALTER TABLE `ubicacion` 
ADD COLUMN `idTransportista` BIGINT(20);

INSERT INTO `ubicacion`(calle, numero, id_Localidad, idTransportista) 
select "ubicacion transportista", 123, localidad.id_Localidad, transportista.id_Transportista from transportista inner join localidad
on transportista.id_Localidad = localidad.id_Localidad;

SET SQL_SAFE_UPDATES = 0;
UPDATE ubicacion u inner join transportista t on u.idTransportista = t.id_Transportista
SET u.descripcion = t.direccion;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE `transportista` 
DROP COLUMN `direccion`;

ALTER TABLE `transportista` 
ADD COLUMN `idUbicacion` BIGINT(20);

UPDATE transportista t
INNER JOIN ubicacion u ON t.id_Transportista = u.idTransportista
SET t.idUbicacion = u.idUbicacion; 

ALTER TABLE `transportista` 
DROP FOREIGN KEY `FK7i066mrrg36mr0olx1eaqbua5`;
ALTER TABLE `transportista` 
DROP COLUMN `id_Localidad`,
DROP INDEX `FK7i066mrrg36mr0olx1eaqbua5` ;

-- FOREING KEY TRANSPORTISTA
ALTER TABLE `transportista`
ADD CONSTRAINT `FKlu1d8169dmth4c4u8u409y0yo` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `ubicacion` 
DROP COLUMN `idTransportista`;

-- EMPRESA

ALTER TABLE `ubicacion` 
ADD COLUMN `idEmpresa` BIGINT(20);

INSERT INTO `ubicacion`(calle, numero, id_Localidad, idEmpresa) 
select "ubicacion empresa", 123, localidad.id_Localidad, empresa.id_Empresa from empresa inner join localidad
on empresa.id_Localidad = localidad.id_Localidad;

SET SQL_SAFE_UPDATES = 0;
UPDATE ubicacion u inner join empresa e on u.idEmpresa = e.id_Empresa
SET u.descripcion = e.direccion;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE `empresa` 
DROP COLUMN `direccion`;

ALTER TABLE `empresa` 
ADD COLUMN `idUbicacion` BIGINT(20);

SET SQL_SAFE_UPDATES = 0;
UPDATE empresa e
INNER JOIN ubicacion u ON e.id_Empresa = u.idEmpresa
SET e.idUbicacion = u.idUbicacion; 
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE `empresa` --
DROP FOREIGN KEY `FK98yi7oddg1up58158pwk9lf39`; 
ALTER TABLE `empresa` 
DROP COLUMN `id_Localidad`,
DROP INDEX `FK98yi7oddg1up58158pwk9lf39` ;

-- FOREING KEY EMPRESA
ALTER TABLE `empresa`
ADD CONSTRAINT `FK9vp5rconju76goo4m612b13vg` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `ubicacion` 
DROP COLUMN `idEmpresa`;

-- TIRAR TABLA PAIS

ALTER TABLE `provincia` 
DROP FOREIGN KEY `FKoeyy00k8sswpaedo6i6dvux4r`; 
ALTER TABLE `provincia` 
DROP COLUMN `id_Pais`,
DROP INDEX `FKoeyy00k8sswpaedo6i6dvux4r` ;

DROP TABLE pais;

-- llenar tabla pedido

ALTER TABLE `pedido` 
ADD COLUMN  `tipoDeEnvio` varchar(255) NOT NULL,
ADD COLUMN  `calle` varchar(255),
ADD COLUMN  `codigoPostal` varchar(255) DEFAULT NULL,
ADD COLUMN  `departamento` varchar(255) DEFAULT NULL,
ADD COLUMN  `descripcion` varchar(255) DEFAULT NULL,
ADD COLUMN  `eliminada` bit(1) NOT NULL,
ADD COLUMN  `idLocalidad` bigint(20) DEFAULT NULL,
ADD COLUMN  `idProvincia` bigint(20) DEFAULT NULL,
ADD COLUMN  `idUbicacion` bigint(20) NOT NULL,
ADD COLUMN  `latitud` double DEFAULT NULL,
ADD COLUMN  `longitud` double DEFAULT NULL,
ADD COLUMN  `nombreLocalidad` varchar(255) DEFAULT NULL,
ADD COLUMN  `nombreProvincia` varchar(255) DEFAULT NULL,
ADD COLUMN  `numero` int(11),
ADD COLUMN  `piso` int(11) DEFAULT NULL;

update pedido 
inner join cliente on pedido.id_Cliente = cliente.id_Cliente
inner join ubicacion on cliente.idUbicacionFacturacion = ubicacion.idUbicacion
inner join localidad on localidad.id_Localidad = ubicacion.id_Localidad
inner join provincia on provincia.id_Provincia = localidad.id_Provincia
SET pedido.calle = ubicacion.descripcion, 
pedido.codigoPostal = localidad.codigoPostal,
pedido.departamento = ubicacion.departamento,
pedido.descripcion = ubicacion.descripcion,
pedido.idLocalidad = localidad.id_Localidad,
pedido.idProvincia = provincia.id_Provincia,
pedido.idUbicacion = ubicacion.idUbicacion,
pedido.latitud = ubicacion.latitud, 
pedido.longitud = ubicacion.longitud, 
pedido.nombreLocalidad = localidad.nombre,
pedido.nombreProvincia = provincia.nombre,
pedido.numero = -9,
pedido.piso = ubicacion.piso,
pedido.tipoDeEnvio = "USAR_UBICACION_FACTURACION"
;

SET SQL_SAFE_UPDATES = 0;
update pedido 
SET pedido.calle = 'backfill calle pedido', 
pedido.numero = 999, 
pedido.tipoDeEnvio = "USAR_UBICACION_FACTURACION"
WHERE pedido.calle is null or pedido.numero is null
;
SET SQL_SAFE_UPDATES = 1;

-- BackFill Codigo postal
SET SQL_SAFE_UPDATES = 0;
UPDATE localidad
SET 
    localidad.codigoPostal = 'N99999'
WHERE
    localidad.codigoPostal = '';
SET SQL_SAFE_UPDATES = 1;

SET SQL_SAFE_UPDATES = 0;
UPDATE localidad
SET 
    localidad.costoEnvio = 0;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE `localidad` 
DROP FOREIGN KEY `FKip25qf9bb8vuf96trysbnng6g`;

ALTER TABLE `localidad` CHANGE `id_Localidad` `idLocalidad` bigint(20) NOT NULL AUTO_INCREMENT; 

ALTER TABLE `localidad` CHANGE `id_Provincia` `idProvincia` bigint(20) NOT NULL; 

ALTER TABLE `provincia` CHANGE `id_Provincia` `idProvincia` bigint(20) NOT NULL AUTO_INCREMENT; 

ALTER TABLE `localidad`
ADD CONSTRAINT `FKip25qf9bb8vuf96trysbnng6g` FOREIGN KEY (`idProvincia`) 
REFERENCES `provincia`(`idProvincia`);

ALTER TABLE `ubicacion` 
DROP FOREIGN KEY `FKnt928a1oc3mtwe2spg8ov6qdm`;

ALTER TABLE `ubicacion` CHANGE `id_Localidad` `idLocalidad` bigint(20) NOT NULL; 

ALTER TABLE `ubicacion`
ADD CONSTRAINT `FKnt928a1oc3mtwe2spg8ov6qdm` FOREIGN KEY (`idLocalidad`) 
REFERENCES `localidad`(`idLocalidad`);

ALTER TABLE `localidad`
DROP COLUMN `eliminada`;

ALTER TABLE `provincia`
DROP COLUMN `eliminada`;

-- Tirar tabla ubicación

ALTER TABLE `cliente` 
DROP FOREIGN KEY `FKkfnh6um3l9l5i0ywxwqr1qq9e`; -- envio

ALTER TABLE `cliente` 
DROP FOREIGN KEY `FK838frolnqaeg8h97ggqu1rd67`; -- facturacion

ALTER TABLE `proveedor` 
DROP FOREIGN KEY `FKjljtmiir6f667w008hwkpqoca`;

ALTER TABLE `transportista` 
DROP FOREIGN KEY `FKlu1d8169dmth4c4u8u409y0yo`;

ALTER TABLE `empresa` 
DROP FOREIGN KEY `FK9vp5rconju76goo4m612b13vg`;

TRUNCATE TABLE `ubicacion`;

SET SQL_SAFE_UPDATES = 0;

UPDATE cliente
SET cliente.idUbicacionEnvio = NULL;

UPDATE cliente
SET cliente.idUbicacionFacturacion = NULL;

UPDATE proveedor
SET proveedor.idUbicacion = NULL;

UPDATE transportista
SET transportista.idUbicacion = NULL;

UPDATE empresa
SET empresa.idUbicacion = NULL;

SET SQL_SAFE_UPDATES = 1;

SET foreign_key_checks = 0;

ALTER TABLE `cliente`
ADD CONSTRAINT `FKkfnh6um3l9l5i0ywxwqr1qq9e` FOREIGN KEY (`idUbicacionEnvio`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `cliente`
ADD CONSTRAINT `FK838frolnqaeg8h97ggqu1rd67` FOREIGN KEY (`idUbicacionFacturacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `proveedor`
ADD CONSTRAINT `FKjljtmiir6f667w008hwkpqoca` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `transportista`
ADD CONSTRAINT `FKlu1d8169dmth4c4u8u409y0yo` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `empresa`
ADD CONSTRAINT `FK9vp5rconju76goo4m612b13vg` FOREIGN KEY (`idUbicacion`) 
REFERENCES `ubicacion`(`idUbicacion`);

ALTER TABLE `ubicacion` 
DROP FOREIGN KEY `FKnt928a1oc3mtwe2spg8ov6qdm`;

TRUNCATE TABLE `localidad`;

ALTER TABLE `localidad` 
DROP FOREIGN KEY `FKip25qf9bb8vuf96trysbnng6g`;

TRUNCATE TABLE `provincia`;

ALTER TABLE `ubicacion`
ADD CONSTRAINT `FKnt928a1oc3mtwe2spg8ov6qdm` FOREIGN KEY (`idLocalidad`) 
REFERENCES `localidad`(`idLocalidad`);

ALTER TABLE `localidad`
ADD CONSTRAINT `FKip25qf9bb8vuf96trysbnng6g` FOREIGN KEY (`idProvincia`) 
REFERENCES `provincia`(`idProvincia`);

SET foreign_key_checks = 1;
