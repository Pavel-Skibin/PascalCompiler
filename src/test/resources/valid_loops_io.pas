program LoopAndIo;
var
  i: integer;
  sum: integer;
begin
  ReadLn(i);
  sum := 0;

  while i > 0 do
  begin
    if i mod 2 = 0 then
      Write(i)
    else
      WriteLn(i);

    sum := sum + i;
    i := i - 1;
  end;

  for i := 1 to 5 do
  begin
    if i = 3 then
      continue;
    if i > 4 then
      break;
    WriteLn(i);
  end;

  repeat
    sum := sum - 1;
  until sum <= 0;

  WriteLn('done');
end.
