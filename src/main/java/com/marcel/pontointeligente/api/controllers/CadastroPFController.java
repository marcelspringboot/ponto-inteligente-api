package com.marcel.pontointeligente.api.controllers;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marcel.pontointeligente.api.domain.Empresa;
import com.marcel.pontointeligente.api.domain.Funcionario;
import com.marcel.pontointeligente.api.dtos.CadastroPFDto;
import com.marcel.pontointeligente.api.enums.PerfilEnum;
import com.marcel.pontointeligente.api.response.Response;
import com.marcel.pontointeligente.api.services.EmpresaService;
import com.marcel.pontointeligente.api.services.FuncionarioService;
import com.marcel.pontointeligente.api.utils.PasswordUtils;

@RestController
@RequestMapping("/api/cadastrar-pf")
@CrossOrigin(origins = "*")
public class CadastroPFController {

	private static final Logger log = LoggerFactory.getLogger(CadastroPFController.class);
	
	@Autowired
	private FuncionarioService funcionarioService;
	
	@Autowired
	private EmpresaService empresaService;
	
	public CadastroPFController() {
	}
	
	/**
	 * Cadastra um funcionário pessoa física no sistema
	 * 
	 * @param cadastroPFDto
	 * @param result
	 * @return ResponseEntity<Response<CadastroPFDto>>
	 * @throws NosuchAlgorithmException
	 */
	
	@PostMapping
	public ResponseEntity<Response<CadastroPFDto>> cadastrar(@Valid @RequestBody CadastroPFDto cadastroPFDto, 
			BindingResult result) throws NoSuchAlgorithmException{
		log.info("Cadastrando PF: {}", cadastroPFDto.toString());
		Response<CadastroPFDto> response = new Response<CadastroPFDto>();
		
		ValidarDadosExistentes(cadastroPFDto, result);
		Funcionario funcionario = this.converterDtoParaFuncionario(cadastroPFDto, result);
		
		if (result.hasErrors()) {
			log.error("Erro validando dados de cadastro PF: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}
		
		Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
		empresa.ifPresent(emp -> funcionario.setEmpresa(emp));
		this.funcionarioService.persistir(funcionario);
		
		response.setData(this.converterCadastroPFDto(funcionario));
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Verifica se a empresa ou funcionário já existem na base de dados.
	 * 
	 * @param cadastroPFDto
	 * @param result
	 */
	
	private void ValidarDadosExistentes(CadastroPFDto cadastroPFDto, BindingResult result) {
		Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
		if (!empresa.isPresent()) {
			result.addError(new ObjectError("empresa", "Empresa não cadastrada."));
		}
		
		this.funcionarioService.buscarPorCpf(cadastroPFDto.getCpf())
			.ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existe.")));
		
		this.funcionarioService.buscarPorEmail(cadastroPFDto.getEmail())
		.ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existe.")));
	}
	
	/**
	 * Converte os dados do DTO para funcionário.
	 * 
	 * @param cadastroPFDto
	 * @param result
	 * @return Funcionario
	 * @throws NoSuchAlgorithmException
	 */
	
	private Funcionario converterDtoParaFuncionario(CadastroPFDto cadastroPFDto, BindingResult result) throws NoSuchAlgorithmException {
		Funcionario funcionario = new Funcionario();
		funcionario.setNome(cadastroPFDto.getNome());
		funcionario.setEmail(cadastroPFDto.getEmail());
		funcionario.setCpf(cadastroPFDto.getCpf());
		funcionario.setPerfil(PerfilEnum.ROLE_USUARIO);
		funcionario.setSenha(PasswordUtils.gerarBCRYPT(cadastroPFDto.getSenha()));
		cadastroPFDto.getQtdHorasAlmoco().ifPresent(qtdHorasAlmoco -> funcionario.setQtdTrabalhoAlmoco(Float.valueOf(qtdHorasAlmoco)));
		cadastroPFDto.getQtdHorasTrabalhoDia().ifPresent(qtdHorasTrabalhoDia -> funcionario.setQtdHorasTrabalhoDia(Float.valueOf(qtdHorasTrabalhoDia)));
		cadastroPFDto.getValorHora().ifPresent(valorHora -> funcionario.setValorHora(new BigDecimal(valorHora)));
		return funcionario;
	}
	
	/**
	 * Popular DTO de cadastro com dados do cuncionário e empresa.
	 * 
	 * @param funcionario
	 * @return CadastroPFDto
	 */
	private CadastroPFDto converterCadastroPFDto(Funcionario funcionario) {
		CadastroPFDto cadastroPFDto = new CadastroPFDto();
		cadastroPFDto.setId(funcionario.getId());
		cadastroPFDto.setNome(funcionario.getNome());
		cadastroPFDto.setCpf(funcionario.getCpf());
		cadastroPFDto.setCnpj(funcionario.getEmpresa().getCnpj());
		funcionario.getQtdHorasTrabalhoDiaOpt().ifPresent(HorasAlmoco -> cadastroPFDto.setQtdHorasAlmoco(Optional.of(Float.toString(HorasAlmoco))));
		funcionario.getQtdHorasTrabalhoDiaOpt().ifPresent(horasTrabDia -> cadastroPFDto.setQtdHorasTrabalhoDia(Optional.of(Float.toString(horasTrabDia))));
		funcionario.getValorHoraOpt().ifPresent(valorHora -> cadastroPFDto.setValorHora(Optional.of(valorHora.toString())));
		
		return cadastroPFDto;
	}
}
